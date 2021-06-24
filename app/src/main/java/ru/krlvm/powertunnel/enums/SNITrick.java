package ru.krlvm.powertunnel.enums;

public enum SNITrick {

    SPOIL,
    ERASE,
    FAKE;

    public static SNITrick fromID(int id) {
        assert id-1 > 0 && id <= values().length;
        return values()[id-1];
    }

    public static final String SUPPORT_REFERENCE = "https://github.com/krlvm/PowerTunnel/wiki/SNI-Tricks";
}