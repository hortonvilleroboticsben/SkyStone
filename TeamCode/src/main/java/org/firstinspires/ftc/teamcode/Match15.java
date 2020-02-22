package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

@Autonomous(name="Match15 AUTO")
public class Match15 extends OpMode {

    StateMachine drive = new StateMachine();
    Robot r = Robot.getInstance();

    @Override
    public void init() {
        drive.reset();
        r.initialize(this);
        telemetry.addData("MATCH 15 ONLY","");
    }

    @Override
    public void loop() {
        drive.initializeMachine();
        drive.pause(5);
        drive.translate(0,0.6, 48);
        drive.translate(180, 0.6, 30);
        drive.translate(-90, 0.6, 24);
    }
}
