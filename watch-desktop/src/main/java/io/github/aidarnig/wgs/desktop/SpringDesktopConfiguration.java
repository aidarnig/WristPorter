package io.github.aidarnig.wgs.desktop;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Headless web runtime: Spring is used only for desktop service wiring. */
@SpringBootApplication(scanBasePackages = "io.github.aidarnig.wgs.core")
public class SpringDesktopConfiguration {}
