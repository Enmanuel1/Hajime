/** Coaching module. Only package-root contracts are exported. */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "identity",
      "organization",
      "practitioner",
      "martialart",
      "training",
      "shared"
    })
package com.hajime.coaching;
