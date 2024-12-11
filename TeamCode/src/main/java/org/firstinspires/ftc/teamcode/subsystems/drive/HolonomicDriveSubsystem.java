package org.firstinspires.ftc.teamcode.subsystems.drive;

import static org.firstinspires.ftc.teamcode.constants.DriveControlLoops.*;

import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.command.RunCommand;
import com.arcrobotics.ftclib.command.Subsystem;
import com.arcrobotics.ftclib.command.WaitUntilCommand;

import org.firstinspires.ftc.teamcode.constants.DriveControlLoops;
import org.firstinspires.ftc.teamcode.constants.DriveTrainConstants;
import org.firstinspires.ftc.teamcode.constants.SystemConstants;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.Trajectory;

public interface HolonomicDriveSubsystem extends Subsystem {
    /**
     * runs a ChassisSpeeds without doing any pre-processing
     * @param speeds a discrete chassis speed, robot-centric
     * */
    void runRawChassisSpeeds(ChassisSpeeds speeds);

    /**
     * Returns the current odometry Pose.
     */
    Pose2d getPose();

    default Pose2d getPoseWithVelocityCompensation(double translationLookAheadTime, double rotationalLookAheadTime) {
        final ChassisSpeeds robotSpeedsFieldRelative = getMeasuredChassisSpeedsFieldRelative();
        final Transform2d lookAheadTransform = new Transform2d(
                robotSpeedsFieldRelative.vxMetersPerSecond * translationLookAheadTime,
                robotSpeedsFieldRelative.vyMetersPerSecond * translationLookAheadTime,
                new Rotation2d(robotSpeedsFieldRelative.omegaRadiansPerSecond * rotationalLookAheadTime));

        return getPose().plus(lookAheadTransform);
    }

    default Rotation2d getFacing() {return getPose().getRotation(); }

    /**
     * Resets the current odometry Pose to a given Pose
     */
    void setPose(Pose2d currentPose);

    /**
     * @return the measured(actual) velocities of the chassis, robot-relative
     * */
    ChassisSpeeds getMeasuredChassisSpeedsRobotRelative();

    default ChassisSpeeds getMeasuredChassisSpeedsFieldRelative() {
        return ChassisSpeeds.fromRobotRelativeSpeeds(getMeasuredChassisSpeedsRobotRelative(), getFacing());
    }

    double getChassisMaxLinearVelocity();
    double getChassisMaxAngularVelocity();

    /**
     * Adds a vision measurement to the pose estimator.
     *
     * @param visionPose The pose of the robot as measured by the vision camera.
     * @param timestamp  The timestamp of the vision measurement in seconds.
     */
    void addVisionMeasurement(Pose2d visionPose, double timestamp);

    /**
     * runs a field-centric ChassisSpeeds
     * @param fieldCentricSpeeds a continuous chassis speeds, field-centric, normally from a pid position controller
     * */
    default void runFieldCentricChassisSpeeds(ChassisSpeeds fieldCentricSpeeds) {
        runRobotCentricChassisSpeeds(ChassisSpeeds.fromFieldRelativeSpeeds(
                fieldCentricSpeeds,
                getPose().getRotation()
        ));
    }

    default void stop() {
        runRobotCentricChassisSpeeds(new ChassisSpeeds());
    }

    /**
     * runs a ChassisSpeeds, pre-processed with ChassisSpeeds.discretize()
     * @param speeds a continuous chassis speed, robot-centric
     * */
    default void runRobotCentricChassisSpeeds(ChassisSpeeds speeds) {
        runRawChassisSpeeds(ChassisSpeeds.discretize(
                speeds,
                1.0/ SystemConstants.ROBOT_UPDATE_RATE_HZ
        ));
    }

    static boolean isZero(ChassisSpeeds chassisSpeeds) {
        return chassisSpeeds.omegaRadiansPerSecond == 0 && chassisSpeeds.vxMetersPerSecond == 0 && chassisSpeeds.vyMetersPerSecond == 0;
    }

    default Command drive(Supplier<ChassisSpeeds> chassisSpeedsSupplier, BooleanSupplier fieldCentricSwitch) {
        return new RunCommand(
                () -> {
                    if (fieldCentricSwitch.getAsBoolean())
                        runFieldCentricChassisSpeeds(chassisSpeedsSupplier.get());
                   else
                       runRobotCentricChassisSpeeds(chassisSpeedsSupplier.get());
                },
                this
        );
    }

    default Command driveToPose(Supplier<Pose2d> target) {
        return drive(
                () -> driveController.calculate(
                        this.getPoseWithVelocityCompensation(
                                DriveControlLoops.TRANSLATIONAL_LOOK_AHEAD_TIME,
                                DriveControlLoops.ROTATIONAL_LOOK_AHEAD_TIME),
                        target.get(),
                        0,
                        target.get().getRotation()),
                () -> false)
                .beforeStarting(() -> driveController.getThetaController().reset(getFacing().getRadians()));
    }

    default Command driveToPose(Supplier<Pose2d> target, Pose2d tolerance, double timeOutSeconds) {
        return driveToPose(target)
                .beforeStarting(() -> driveController.setTolerance(tolerance))
                .raceWith(new WaitUntilCommand(driveController::atReference))
                .withTimeout((long)(timeOutSeconds * 1000))
                .andThen(new InstantCommand(this::stop));
    }


    default Command followTrajectory(Supplier<Trajectory.State> desiredState, Supplier<Rotation2d> desiredRotation) {
        return drive(
                () -> driveController.calculate(
                        this.getPoseWithVelocityCompensation(
                                DriveControlLoops.TRANSLATIONAL_LOOK_AHEAD_TIME,
                                DriveControlLoops.ROTATIONAL_LOOK_AHEAD_TIME),
                        desiredState.get(),
                        desiredRotation.get()),
                () -> false);
    }
}
