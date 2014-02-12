package com.gpl.rpg.AndorsTrail.model.io;

import com.gpl.rpg.AndorsTrail.util.Range;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Gabriel on 11/02/14.
 */
public class RangeWriter {
    public void writeToParcel(Range range, DataOutputStream dest) throws IOException {
        dest.writeInt(range.max);
        dest.writeInt(range.current);
    }

}
