package io.github.aidarnig.wgs.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * Test-only owner-run Windows/iPhone reachability probe, NOT part of a release artifact.
 * No uploads, ADB calls, watch discovery or remote management.
 *
 * Compile: mvn -B -ntp -pl watch-web -am test
 * Run on Windows (Java 25):
 * java -cp "watch-web\\target\\classes;watch-web\\target\\test-classes"
 *      io.github.aidarnig.wgs.web.GatewayProbeMain 192.168.1.123
 */
public final class GatewayProbeMain {
    private GatewayProbeMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Provide the Windows PC's trusted private Wi-Fi IPv4 address as one argument.");
            System.exit(2);
        }
        InetAddress candidate = InetAddress.getByName(args[0]);
        if (!(candidate instanceof Inet4Address address)
                || !candidate.getHostAddress().equals(args[0])) {
            throw new IllegalArgumentException("Use a literal IPv4 address, not a hostname");
        }
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(LanUploadGateway.PLAINTEXT_WARNING);
        System.out.println("TEST-ONLY: No photos, ADB or watch operations are available.");
        System.out.println("Do you control and trust this LAN and accept plaintext HTTP? Type I UNDERSTAND:");
        if (!"I UNDERSTAND".equals(console.readLine())) {
            System.out.println("Not started; no network listener created.");
            return;
        }
        // This is a synthetic watch lease for the no-upload probe ONLY. The eventual
        // Desktop integration MUST use an actual selected/ready watch and live lease.
        try (LanUploadGateway gateway = new LanUploadGateway()) {
            LanUploadGateway.OwnerAccess access = gateway.start(new LanUploadGateway.StartRequest(
                    address, "TEST-ONLY-NO-WATCH", () -> "TEST-ONLY-NO-WATCH",
                    true, true, true));
            System.out.println("Test-only URL for iPhone Safari: " + access.url());
            System.out.println("One-time PIN (expires in 2 minutes): " + access.oneTimePin());
            System.out.println("Expected: Safari shows a test form; submit PIN; result says 'Paired'.");
            System.out.println("Press ENTER to revoke and stop the server. Ctrl+C also ends this process.");
            console.readLine();
        }
        System.out.println("Gateway stopped.");
    }
}
