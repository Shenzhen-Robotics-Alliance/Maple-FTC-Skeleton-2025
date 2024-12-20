package org.firstinspires.ftc.teamcode;

import com.arcrobotics.ftclib.command.Robot;
import com.arcrobotics.ftclib.command.button.Trigger;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.commands.drive.JoystickDriveFactory;
import org.firstinspires.ftc.teamcode.utils.MapleJoystickDriveInput;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;

/**
 * robot during teleop stage'
 * here we bind all the commands to a key on the GamePad
 * */
public class TeleOpRobot extends Robot {
    private final RobotContainer robotContainer;
    private final GamepadEx pilotGamePad, copilotGamePad;
    private final Runnable calibrateOdometry;
    public TeleOpRobot(RobotContainer robotContainer, Gamepad pilotGamePad, Gamepad copilotGamePad) {
        super();
        this.robotContainer = robotContainer;
        this.pilotGamePad = new GamepadEx(pilotGamePad);
        this.copilotGamePad = new GamepadEx(copilotGamePad);

        this.calibrateOdometry = () -> robotContainer.driveSubsystem.setPose(new Pose2d());
        calibrateOdometry.run();
        configureKeyBindings();
    }

    private void configureKeyBindings() {
        this.pilotGamePad.getGamepadButton(GamepadKeys.Button.RIGHT_STICK_BUTTON).whenHeld(JoystickDriveFactory.joystickDrive(
                robotContainer.driveSubsystem,
                MapleJoystickDriveInput.leftHandedJoystick(pilotGamePad),
                () -> true));

//        robotContainer.driveSubsystem.setDefaultCommand(JoystickDriveFactory.joystickDrive(
//                robotContainer.driveSubsystem,
//                MapleJoystickDriveInput.leftHandedJoystick(pilotGamePad),
//                () -> -pilotGamePad.getRightY(),
//                () -> -pilotGamePad.getRightX()));

        robotContainer.driveSubsystem.setDefaultCommand(JoystickDriveFactory.joystickDrive(
                robotContainer.driveSubsystem,
                MapleJoystickDriveInput.leftHandedJoystick(pilotGamePad),
                () -> true));

        this.pilotGamePad.getGamepadButton(GamepadKeys.Button.START).whenPressed(calibrateOdometry);

        this.pilotGamePad.getGamepadButton(GamepadKeys.Button.B).whenHeld(robotContainer.driveSubsystem.followPath(
                new Pose2d[]{
                        new Pose2d(0, 0, Rotation2d.fromDegrees(45)),
                        new Pose2d(0.5, 0.5, Rotation2d.fromDegrees(90)),
                        new Pose2d(0.5, 1, Rotation2d.fromDegrees(90))},
                Rotation2d.fromDegrees(0),
                0.5));

        new Trigger(() -> this.pilotGamePad.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER) > 0.5)
                .whileActiveOnce(robotContainer.driveSubsystem.driveToPose(() -> new Pose2d(0, 0, Rotation2d.fromDegrees(0)), new Pose2d(0.02, 0.02, Rotation2d.fromDegrees(5)), 2));
    }

    @Override
    public void reset() {
        super.reset();
        try {
            robotContainer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
