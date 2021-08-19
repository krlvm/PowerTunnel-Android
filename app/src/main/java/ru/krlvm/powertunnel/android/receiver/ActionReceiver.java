/*
 * This file is part of PowerTunnel-Android.
 *
 * PowerTunnel-Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerTunnel-Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerTunnel-Android.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.krlvm.powertunnel.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.krlvm.powertunnel.android.managers.PTManager;

public class ActionReceiver extends BroadcastReceiver {

    public static final String ACTION_START_TUNNEL = "ru.krlvm.powertunnel.android.action.START_TUNNEL";
    public static final String ACTION_STOP_TUNNEL = "ru.krlvm.powertunnel.android.action.STOP_TUNNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() == null) return;

        switch (intent.getAction()) {
            case ACTION_START_TUNNEL: {
                PTManager.startTunnel(context);
                break;
            }
            case ACTION_STOP_TUNNEL: {
                PTManager.stopTunnel(context);
                break;
            }
        }
    }
}
