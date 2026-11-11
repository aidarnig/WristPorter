package io.github.aidarnig.wgs.adb;

/** Validated wireless ADB endpoint. Pairing and debugging typically use different ports. */
public record WatchEndpoint(String host, int port) {
    public static WatchEndpoint parse(String value) {
        if (value == null || !value.matches("[A-Za-z0-9.\\-]+:[0-9]{1,5}")) {
            throw new IllegalArgumentException("Укажите IP:порт, например 192.168.1.101:38267");
        }
        int separator = value.lastIndexOf(':');
        int port = Integer.parseInt(value.substring(separator + 1));
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Номер порта должен быть от 1 до 65535");
        }
        String host = value.substring(0, separator);
        if (host.startsWith("-") || host.endsWith("-") || host.contains("..")) {
            throw new IllegalArgumentException("Неверный IP-адрес или имя хоста");
        }
        return new WatchEndpoint(host, port);
    }

    @Override public String toString() {
        return host + ":" + port;
    }
}
