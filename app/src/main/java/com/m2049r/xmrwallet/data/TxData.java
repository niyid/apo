/*
 * Copyright (c) 2017 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.m2049r.xmrwallet.model.CoinsInfo;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// https://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents
@ToString
public class TxData implements Parcelable {
    @Getter
    private String[] destinations = new String[1];
    @Getter
    private long[] amounts = new long[1];
    @Getter
    @Setter
    private int mixin;
    @Getter
    @Setter
    private PendingTransaction.Priority priority;
    @Getter
    private int[] subaddresses;

    @Getter
    @Setter
    private UserNotes userNotes;

    public TxData() {
    }

    public String getDestination() {
        return destinations[0];
    }

    public long getAmount() {
        return amounts[0];
    }

    public long getPocketChangeAmount() {
        long change = 0;
        for (int i = 1; i < amounts.length; i++) {
            change += amounts[i];
        }
        return change;
    }

    public void setDestination(String destination) {
        destinations[0] = destination;
    }

    public void setAmount(long amount) {
        amounts[0] = amount;
    }

    public void setAmount(double amount) {
        setAmount(Wallet.getAmountFromDouble(amount));
    }

    private void resetPocketChange() {
        if (destinations.length > 1) {
            final String destination = getDestination();
            destinations = new String[1];
            destinations[0] = destination;
            final long amount = getAmount();
            amounts = new long[1];
            amounts[0] = amount;
        }
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(destinations.length);
        out.writeStringArray(destinations);
        out.writeLongArray(amounts);
        out.writeInt(mixin);
        out.writeInt(priority.getValue());
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<TxData> CREATOR = new Parcelable.Creator<TxData>() {
        public TxData createFromParcel(Parcel in) {
            return new TxData(in);
        }

        public TxData[] newArray(int size) {
            return new TxData[size];
        }
    };

    protected TxData(Parcel in) {
        int len = in.readInt();
        destinations = new String[len];
        in.readStringArray(destinations);
        amounts = new long[len];
        in.readLongArray(amounts);
        mixin = in.readInt();
        priority = PendingTransaction.Priority.fromInteger(in.readInt());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
