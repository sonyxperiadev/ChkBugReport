package com.sonyericsson.chkbugreport.plugins.logs.kernel.iptables;

public class IPUtils {

    // Source: http://en.wikipedia.org/w/index.php?title=IPv4#Special-use_addresses
    public static final String RANGES[][] = {
        { "255.255.255.255/32", "Broadcast", },
        { "0.0.0.0/8", "Current network", },
        { "10.0.0.0/8", "Private network", },
        { "100.64.0.0/10", "Shared Address Space", },
        { "127.0.0.0/8", "Loopback", },
        { "169.254.0.0/16", "Link-local", },
        { "172.16.0.0/12", "Private network", },
        { "192.0.0.0/24", "IETF Protocol Assignments", },
        { "192.0.2.0/24", "TEST-NET-1", },
        { "192.88.99.0/24", "IPv6 to IPv4 relay", },
        { "192.168.0.0/16", "Private network", },
        { "198.18.0.0/15", "Network benchmark tests", },
        { "198.51.100.0/24", "TEST-NET-2", },
        { "203.0.113.0/24", "TEST-NET-3", },
        { "224.0.0.0/4", "IP multicast", },
        { "240.0.0.0/4", "Reserved", },
    };

    public static int sAddrs[];
    public static int sMasks[];

    private static void compile() {
        if (sAddrs != null && sMasks != null) return;
        int len = RANGES.length;
        sAddrs = new int[len];
        sMasks = new int[len];
        for (int i = 0; i < len; i++) {
            String s[] = RANGES[i][0].split("/");
            sAddrs[i] = compileIp(s[0]);
            sMasks[i] = buildMask(Integer.parseInt(s[1]));
        }
    }

    public static String getIpRangeName(String ip) {
        compile();
        int value = compileIp(ip);
        for (int i = 0; i < RANGES.length; i++) {
            if (sAddrs[i] == (value & sMasks[i])) {
                return RANGES[i][1];
            }
        }
        // No special address, so it must be a public internet address
        return "Internet";
    }

    public static int buildMask(int bits) {
        if (bits == 32) {
            return 0xffffffff;
        }
        int ones = (1 << bits) - 1;
        return ones << (32 - bits);
    }

    public static int compileIp(String ip) {
        String b[] = ip.split("\\.");
        if (b.length != 4) {
            throw new RuntimeException("invalid IP address: " + ip);
        }
        int ret = Integer.parseInt(b[0]);
        ret = (ret << 8) | Integer.parseInt(b[1]);
        ret = (ret << 8) | Integer.parseInt(b[2]);
        ret = (ret << 8) | Integer.parseInt(b[3]);
        return ret;
    }
}
