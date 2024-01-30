package jp.knowlsat.lstm;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;

public class LSTM_Load {
    public static FileInputStream fis;
    public static DataInputStream dis;

    public static int load(LSTM lstm) {
        openFile();

        loadHiddenLayer(lstm.hiddenLayer[0], dis);
        loadOutputLayer(lstm.outputLayer, dis);

        closeFile();

        return 0;
    }

    public static int openFile() {
		String fileName = "lstm.save";

        Path filePath = Path.of("setting", fileName);
		File file = filePath.toFile();

		try {
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
        } catch (Exception e) {
			System.out.println(e.getMessage());
            return -1;
		}

        return 0;
    }

    public static int closeFile() {
            try {
            if (dis != null) {
                dis.close();
            }

            if (fis != null) {
                fis.close();
            }
        } catch (Exception e) {
			System.out.println(e.getMessage());
            return -1;
		}

        return 0;
    }

    public static int loadHiddenLayer(LSTM_HiddenLayer hiddenLayer, DataInputStream dis) {
        try {
            hiddenLayer.nIn = dis.readInt();
            hiddenLayer.nHidden = dis.readInt();

            for (int i = 0; i < hiddenLayer.nHidden; i++) {
                hiddenLayer.A[i] = dis.readDouble();
                hiddenLayer.B[i] = dis.readDouble();
                hiddenLayer.A2[i] = dis.readDouble();
                hiddenLayer.B2[i] = dis.readDouble();
                hiddenLayer.BF[i] = dis.readDouble();
                hiddenLayer.BI[i] = dis.readDouble();
                hiddenLayer.BG[i] = dis.readDouble();
                hiddenLayer.BO[i] = dis.readDouble();
            }

            for (int i = 0; i < hiddenLayer.nIn * hiddenLayer.nHidden; i++) {
                hiddenLayer.Wif[i] = dis.readDouble();
                hiddenLayer.Wii[i] = dis.readDouble();
                hiddenLayer.Wig[i] = dis.readDouble();
                hiddenLayer.Wio[i] = dis.readDouble();
            }

            for (int i = 0; i < hiddenLayer.nHidden * hiddenLayer.nHidden; i++) {
                hiddenLayer.Whf[i] = dis.readDouble();
                hiddenLayer.Whi[i] = dis.readDouble();
                hiddenLayer.Whg[i] = dis.readDouble();
                hiddenLayer.Who[i] = dis.readDouble();
            }

            for (int i = 0; i < hiddenLayer.nHidden; i++) {
                hiddenLayer.Wcf[i] = dis.readDouble();
                hiddenLayer.Wci[i] = dis.readDouble();
                hiddenLayer.Wco[i] = dis.readDouble();
            }
        } catch (Exception e) {
            System.out.println("Failed to read. " + e.getMessage());
            return -1;
        }

        return 0;
    }

    public static int loadOutputLayer(LSTM_OutputLayer outputLayer, DataInputStream dis) {
        try {
            outputLayer.nHidden = dis.readInt();
            outputLayer.nOut = dis.readInt();

            for (int i = 0; i < outputLayer.nOut; i++) {
                outputLayer.B[i] = dis.readDouble();
            }

            for (int i = 0; i < outputLayer.nHidden * outputLayer.nOut; i++) {
                outputLayer.W[i] = dis.readDouble();
            }
        } catch (Exception e) {
            System.out.println("Failed to read. " + e.getMessage());
            return -1;
        }

        return 0;
    }

}
