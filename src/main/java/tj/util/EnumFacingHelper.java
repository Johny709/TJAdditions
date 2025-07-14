package tj.util;

import net.minecraft.util.EnumFacing;

public class EnumFacingHelper {

    public static EnumFacing getTopFacingFrom(EnumFacing facing) {
        switch (facing) {
            case UP:
            case DOWN: return facing.rotateAround(EnumFacing.Axis.X);
            case SOUTH: return facing.rotateAround(EnumFacing.Axis.Y).rotateAround(EnumFacing.Axis.Z);
            case NORTH: return facing.rotateAround(EnumFacing.Axis.Y).rotateAround(EnumFacing.Axis.Z).getOpposite();
            case EAST: return facing.rotateAround(EnumFacing.Axis.Z).getOpposite();
            default: return facing.rotateAround(EnumFacing.Axis.Z);
        }
    }

    public static EnumFacing getBottomFacingFrom(EnumFacing facing) {
        return getTopFacingFrom(facing).getOpposite();
    }

    public static EnumFacing getLeftFacingFrom(EnumFacing facing) {
        switch (facing) {
            case UP: return facing.rotateAround(EnumFacing.Axis.Z).getOpposite();
            case DOWN: return facing.rotateAround(EnumFacing.Axis.Z);
            default: return facing.rotateY();
        }
    }

    public static EnumFacing getRightFacingFrom(EnumFacing facing) {
        return getLeftFacingFrom(facing).getOpposite();
    }
}
