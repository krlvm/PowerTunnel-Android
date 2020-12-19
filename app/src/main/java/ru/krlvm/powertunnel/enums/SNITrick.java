package ru.krlvm.powertunnel.enums;

public enum SNITrick {

    ERASE,
    SPOIL,
    FAKE;

    public static SNITrick fromID(int id) {
        switch (id) {
            case 1: {
                return SPOIL;
            }
            case 2: {
                return ERASE;
            }
            case 3: {
                return FAKE;
            }
            default: {
                return null;
            }
        }
    }

    public static final String SUPPORT_REFERENCE = "https://github.com/krlvm/PowerTunnel/wiki/SNI-Tricks";
}