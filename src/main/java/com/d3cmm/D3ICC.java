package com.d3cmm;

import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/*
 * The Main ICC class, mainly for extracting primaries and the xyz matricies
 */

public class D3ICC{

    ICC_Profile profile;
    ICC_ColorSpace colorSpace;
    double[] white_point;
    double[] black_point;
    double[] r_colorant;
    double[] g_colorant;
    double[] b_colorant;
	double trc;

    RealMatrix to_xyz;
	RealMatrix from_xyz;


    public D3ICC(ICC_Profile profile){
        this.profile = profile;
		this.colorSpace = new ICC_ColorSpace(profile);
		white_point = readColorTristimTag(profile, TAG.WHITE_POINT);
		black_point = readColorTristimTag(profile, TAG.BLACK_POINT);
		r_colorant = readColorTristimTag(profile, TAG.RED_COLORANT);
		g_colorant = readColorTristimTag(profile, TAG.GREEN_COLORANT);
		b_colorant = readColorTristimTag(profile, TAG.BLUE_COLORANT);
		double[][] matrix = {r_colorant, g_colorant, b_colorant};
		to_xyz = MatrixUtils.createRealMatrix(matrix);
		from_xyz = MatrixUtils.inverse(to_xyz);
		trc = readTRC(profile, TAG.RED_TRC);

    }
	public RealMatrix toXYZ(double[] lin_rgb){
		RealMatrix in = MatrixUtils.createColumnRealMatrix(lin_rgb);
		return to_xyz.multiply(in);
	}
	public RealMatrix fromXYZ(double[] lin_rgb){
		RealMatrix in = MatrixUtils.createColumnRealMatrix(lin_rgb);
		return from_xyz.multiply(in);
	}
	public String toString(){
		StringBuilder out = new StringBuilder();
		out.append(                 "" + getName() + "\n");
		out.append(String.format("  Channel    |  R    G    B\n"));
		out.append(String.format("-----------------------------\n"));
		out.append(String.format("white_point  | %.3f %.3f %.3f\n", white_point[0], white_point[1], white_point[2]));
		out.append(String.format("black_point  | %.3f %.3f %.3f\n", black_point[0], black_point[1], black_point[2]));
		out.append(String.format("r_colorant   | %.3f %.3f %.3f\n", r_colorant[0], r_colorant[1], r_colorant[2]));
		out.append(String.format("g_colorant   | %.3f %.3f %.3f\n", g_colorant[0], g_colorant[1], g_colorant[2]));
		out.append(String.format("b_colorant   | %.3f %.3f %.3f\n", b_colorant[0], b_colorant[1], b_colorant[2]));
		out.append(String.format("-----------------------------\n"));
		out.append(String.format("ToXYZ Matrix   \n"));
		out.append(printMatrix(to_xyz.getData()));
		out.append("\n");
		out.append(String.format("FromXYZ Matrix   \n"));
		out.append(printMatrix(from_xyz.getData()));
		out.append("\n");
		return out.toString();
	}
	public static String printMatrix(double[][] array) {
		StringBuilder out = new StringBuilder();
        for (double[] row : array) {
			out.append("[");
            // System.out.print("[");
            for (int i = 0; i < row.length; i++) {
				out.append(String.format("%.3f", row[i]));
                //System.out.printf("%.2f", row[i]); // Format double to two decimal places
                if (i < row.length - 1) {
                    //System.out.print(", ");
					out.append(", ");
                }
            }
			out.append("]\n");
            //System.out.println("]");
        }
		return out.toString();
    }

	public String getName(){
		byte[] name = profile.getData(ICC_Profile.icSigProfileDescriptionTag);
		ByteBuffer buffer = ByteBuffer.wrap(name);
		buffer.getInt();
		buffer.getInt();
		int len = buffer.getInt();
		return new String(name, 12, len);
	}


    public static double readTRC(ICC_Profile profile, TAG tag){
		byte[] tristim = profile.getData(ICC_Profile.icSigRedTRCTag);
		ByteBuffer buffer = ByteBuffer.wrap(tristim);
		buffer.order(ByteOrder.BIG_ENDIAN);
		byte[] type = ByteBuffer.allocate(4).putInt(buffer.getInt()).array();
		buffer.getInt();
		buffer.getInt();
		return (double) (buffer.getShort() / 255.0);
   }


   public static double[] readColorTristimTag(ICC_Profile profile, TAG tag){
		byte[] tristim = profile.getData(tag.getValue());
		ByteBuffer buffer = ByteBuffer.wrap(tristim);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.getInt();
		buffer.getInt();
		double x = round2((double) (buffer.getInt() / 65536.0), 3);
		double y = round2((double) (buffer.getInt() / 65536.0), 3);
		double z = round2((double) (buffer.getInt() / 65536.0), 3);
		return new double[]{x,y,z};
   }
   public static double round2(double number, int scale) {
		int pow = 10;
		for (int i = 1; i < scale; i++)
			pow *= 10;
		double tmp = number * pow;
		return ( (double) ( (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) ) ) / pow;
	}
}

enum TAG{
	WHITE_POINT(ICC_Profile.icSigMediaWhitePointTag),
	BLACK_POINT(ICC_Profile.icSigMediaBlackPointTag),
	RED_COLORANT(ICC_Profile.icSigRedColorantTag),
	GREEN_COLORANT(ICC_Profile.icSigGreenColorantTag),
	BLUE_COLORANT(ICC_Profile.icSigBlueColorantTag),
	RED_TRC(ICC_Profile.icSigRedTRCTag)
	;

	private final int value;
    TAG(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
	
}
