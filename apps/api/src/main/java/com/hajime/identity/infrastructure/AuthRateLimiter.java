package com.hajime.identity.infrastructure;

import com.hajime.shared.BusinessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class AuthRateLimiter {
  private final JdbcClient db;
  private final AuthSettings settings;
  private final TransactionTemplate tx;

  public AuthRateLimiter(JdbcClient db, AuthSettings settings, PlatformTransactionManager manager) {
    this.db = db;
    this.settings = settings;
    tx = new TransactionTemplate(manager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public void ip(String address) {
    check("ip:" + address, settings.maxIpAttempts());
  }

  public void account(String email) {
    check("account:" + email, settings.maxAccountAttempts());
  }

  private void check(String key, int limit) {
    int count =
        tx.execute(
            status -> {
              db.sql(
                      "delete from auth_rate_limit where bucket_hash in (select bucket_hash from auth_rate_limit where window_start<clock_timestamp()-interval '1 day' limit 100)")
                  .update();
              return db.sql(
                      """
       insert into auth_rate_limit(bucket_hash,window_start,attempts) values(:hash,clock_timestamp(),1)
       on conflict(bucket_hash) do update set
         attempts=case when auth_rate_limit.window_start<=clock_timestamp()-(:window * interval '1 second') then 1 else auth_rate_limit.attempts+1 end,
         window_start=case when auth_rate_limit.window_start<=clock_timestamp()-(:window * interval '1 second') then clock_timestamp() else auth_rate_limit.window_start end
       returning attempts
       """)
                  .param("hash", RefreshTokens.hash(key))
                  .param("window", settings.rateWindow().toSeconds())
                  .query(Integer.class)
                  .single();
            });
    if (count > limit)
      throw BusinessException.rateLimited("RATE_LIMITED", "Too many attempts. Try again later.");
  }
}
