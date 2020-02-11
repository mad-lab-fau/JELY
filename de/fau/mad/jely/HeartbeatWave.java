/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 *
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.lme.ecglib;

public class HeartbeatWave extends WaveMorphology
{
    private Heartbeat mHeartbeat = null;

    public HeartbeatWave()
    {
    }

    public HeartbeatWave( Heartbeat beat )
    {
	mHeartbeat = beat;
    }

    public Heartbeat getHeartbeat()
    {
	return mHeartbeat;
    }

    public void setHeartbeat( Heartbeat beat )
    {
	mHeartbeat = beat;
    }
}
