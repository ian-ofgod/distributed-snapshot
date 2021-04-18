package oilwells;

import java.io.Serializable;

/**
 * It is used as a message to transport oil between oil wells
 * */
public class OilCargo implements Serializable {
    /**
     * The oil amount that is being transferred
     * */
    private final int oilAmount;

    public OilCargo(int oilAmount) {
        this.oilAmount = oilAmount;
    }

    public int getOilAmount() {
        return oilAmount;
    }
}
