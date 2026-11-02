/** Audit module. Only package-root contracts are exported. */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"training", "coaching", "organization"})
package com.hajime.audit;
