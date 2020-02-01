package org.firstinspires.ftc.teamcode;

import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;


@TeleOp(name = "Foundation Test", group ="test")
public class FoundationTest extends OpMode {

    Robot r;
    public boolean auto = false;
    boolean toggle, OSFoundationServos = false;
    StateMachine m = new StateMachine();
    double theta1 = 0;

    @Override
    public void init() {
        m.state_in_progress = 99;
        r = Robot.getInstance();
        r.initialize(this);
    }

    @Override
    public void loop() {

        m.initializeMachine();

        if (!auto) {
            //No turning while translating
            if (Math.abs(gamepad1.right_stick_x) < 0.05) {

                double x = gamepad1.left_stick_x;
                double y = -gamepad1.left_stick_y;

                theta1 = ((Math.atan(y / x)));
                //This series of if statements prevents us from dividing by 0
                //Because we divide by X, X != 0
                if (x == 0 && y > 0) {
                    theta1 = Math.PI / 2;
                } else if (x == 0 && y < 0) {
                    theta1 = 3 * Math.PI / 2;
                } else if (x < 0) {
                    theta1 = Math.atan(y / x) + Math.PI;
                }
                double theta2 = Math.PI / 4 - theta1;

                //I JUST NEED THIS COMMENT TO FORCE PUSH
                r.setPower(r.wheelSet1[0], Math.abs(x) > .05 || Math.abs(y) > .05 ? Math.sqrt(x * x + y * y) * Math.cos(theta2) : 0);
                r.setPower(r.wheelSet2[0], Math.abs(x) > .05 || Math.abs(y) > .05 ? -Math.sqrt(x * x + y * y) * Math.sin(theta2) : 0);
                r.setPower(r.wheelSet1[1], Math.abs(x) > .05 || Math.abs(y) > .05 ? Math.sqrt(x * x + y * y) * Math.cos(theta2) : 0);
                r.setPower(r.wheelSet2[1], Math.abs(x) > .05 || Math.abs(y) > .05 ? -Math.sqrt(x * x + y * y) * Math.sin(theta2) : 0);


            }else {
                r.setPower(r.wheelSetL[0], gamepad1.right_stick_x);
                r.setPower(r.wheelSetL[1], gamepad1.right_stick_x);
                r.setPower(r.wheelSetR[0], -gamepad1.right_stick_x);
                r.setPower(r.wheelSetR[1], -gamepad1.right_stick_x);
            }

            if(gamepad1.x && !OSFoundationServos) {
                OSFoundationServos = true;
                toggle = !toggle;
            } else if(!gamepad1.x){
                OSFoundationServos = false;
            }
            if(toggle){
                r.setServoPosition("srvFoundationLeft", .7);
                r.setServoPosition("srvFoundationRight", .7);
            } else if(!toggle) {
                r.setServoPosition("srvFoundationLeft", .2);
                r.setServoPosition("srvFoundationRight", .2);
            }
        }

        if(gamepad1.a && !gamepad1.start){
            auto = true;
            m.reset();
            r.resetDriveEncoders();

        }

        m.translate(0,.5, 20);
        if(m.next_state_to_execute() && auto){
            auto = false;
            m.incrementState();
        }

        telemetry.addData("mtrFrontLeft", r.getEncoderCounts("mtrFrontLeft"));
        telemetry.addData("mtrFrontRight", r.getEncoderCounts("mtrFrontRight"));
        telemetry.addData("mtrBackLeft", r.getEncoderCounts("mtrBackLeft"));
        telemetry.addData("mtrBackRight", r.getEncoderCounts("mtrBackRight"));

        telemetry.addData("theta1", theta1 * 180 / Math.PI);
        telemetry.addData("SIP", m.state_in_progress);
        telemetry.addData("Auto", auto);
    }
}
