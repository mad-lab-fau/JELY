/**
 * This file is part of the JELY distribution (https://github.com/mad-lab-fau/JELY).
 * Copyright (c) 2015-2020 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 * <p>
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.fau.mad.jely.io;

import java.io.*;
import java.util.ArrayList;

/**
 * @author Axel Heinrich
 */
public class ProtocolReader {

    int protocol[];

    public ProtocolReader() {
    }

    public ProtocolReader(String pathToFile) {
        int protocol[] = new int[4];
        LoadFile(pathToFile);
    }

    public int[] LoadFile(String pathToFile) {

        protocol = new int[4];
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(pathToFile)));

            for (String line; (line = br.readLine()) != null; ) {
                line = line.trim();

                // empty lines
                if (line.isEmpty())
                    continue;

                // comment lines
                if (line.charAt(0) == '#') {
                    continue;
                }

                // parse line
                String[] splits = line.split(", ");
                Double sample = null;
                try {
                    if (splits.length < 1)
                        continue;

                    protocol[0] = Integer.parseInt(splits[0]);
                    if (splits.length > 1)
                        protocol[1] = Integer.parseInt(splits[1]);
                    if (splits.length > 2)
                        protocol[2] = Integer.parseInt(splits[2]);
                    if (splits.length > 3)
                        protocol[3] = Integer.parseInt(splits[3]);
                } catch (NumberFormatException e) {
                } catch (NullPointerException e) {
                }
            }

            br.close();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return protocol;
    }

    public int[] getProtocol() {
        return protocol;
    }
}

