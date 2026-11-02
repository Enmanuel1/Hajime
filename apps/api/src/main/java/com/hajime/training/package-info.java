/** Training module. Only package-root contracts are exported. */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"identity", "organization", "practitioner", "martialart", "shared"})
package com.hajime.training;
