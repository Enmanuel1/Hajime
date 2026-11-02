/** Audit module. Only package-root contracts are exported. */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"training", "coaching", "organization", "identity"})
package com.hajime.audit;
