#!/usr/bin/env python3
"""Exercise the dev-only HAJIME vertical slice against a running API (stdlib only)."""
import argparse
import datetime
import json
import urllib.error
import urllib.request
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default="http://localhost:8080")
    args = parser.parse_args()
    base = args.url.rstrip("/")
    dojo = "30000000-0000-0000-0000-000000000001"
    profile = "40000000-0000-0000-0000-000000000001"
    technique = "14000000-0000-0000-0000-000000000008"

    def call(method, path, body=None, user="athlete", expected=200):
        data = None if body is None else json.dumps(body).encode("utf-8")
        request = urllib.request.Request(
            base + path, data=data, method=method,
            headers={"Content-Type": "application/json", "X-Dev-User": user},
        )
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                status, payload = response.status, response.read()
        except urllib.error.HTTPError as error:
            status, payload = error.code, error.read()
        if status != expected:
            raise RuntimeError(f"{method} {path}: expected {expected}, got {status}: {payload.decode()}")
        return json.loads(payload)

    call("GET", "/api/v1/me")
    call("GET", "/api/v1/me/practitioner-profiles")
    call("GET", "/api/v1/martial-arts/JUDO/techniques")
    session_id = str(uuid.uuid4())
    path = "/api/v1/me/training-sessions/" + session_id
    call("POST", "/api/v1/me/training-sessions", {
        "id": session_id, "practitionerProfileId": profile, "organizationId": dojo,
        "occurredAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "durationMinutes": 90, "practiceType": "MIXED", "perceivedIntensity": 6,
        "energyLevel": 4, "reflection": "Maintained sleeve control. Reap timing needs work.",
        "techniques": [{"techniqueId": technique, "repetitions": 20}],
        "sparring": [{"rounds": 4, "intensity": 6}],
    }, expected=201)
    call("GET", "/api/v1/me/training-sessions")
    session = call("GET", path)
    call("POST", path + "/share", {"version": session["version"]})
    review_path = f"/api/v1/organizations/{dojo}/training-reviews"
    review = None
    page = 0
    while review is None:
        result = call("GET", review_path + f"?size=100&page={page}", user="sensei")
        review = next((r for r in result["content"] if r["trainingSessionId"] == session_id), None)
        if review is None and (page + 1) * 100 >= result["totalElements"]:
            raise RuntimeError("Shared session did not appear in pending reviews")
        page += 1
    detail = call("GET", review_path + "/" + review["id"], user="sensei")
    assert detail["training"]["id"] == session_id
    result = call("POST", review_path + "/" + review["id"] + "/feedback", {
        "version": review["version"], "body": "Bring the supporting foot closer before the reap.",
        "goal": {"title": "Balanced O-soto-gari entry", "objective": "Demonstrate a stable entry in three supervised sessions.",
                 "relatedTechniqueId": technique, "priority": "HIGH"},
    }, user="sensei", expected=201)
    feedback = call("GET", "/api/v1/me/feedback?trainingSessionId=" + session_id)
    assert feedback["totalElements"] == 1
    goals = call("GET", "/api/v1/me/goals")
    assert goals["totalElements"] >= 1
    goal = call("GET", "/api/v1/me/goals/" + result["goal"]["id"])
    assert goal["status"] == "ACTIVE"
    assert call("GET", path)["status"] == "REVIEWED"
    call("GET", review_path, user="outsider", expected=403)
    call("GET", "/api/v1/me/goals/" + goal["id"], user="outsider", expected=404)
    print(f"PASS: training {session_id} → coach review → feedback → goal {goal['id']}; organization isolation verified.")


if __name__ == "__main__":
    main()
