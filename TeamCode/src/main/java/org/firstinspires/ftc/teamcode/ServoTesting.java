package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;


@TeleOp(name = "Servo Testing", group ="Testing")
public class ServoTesting extends OpMode {

    Robot r;
    Servo srvFoundLeft, srvFoundRight;
    boolean OSFoundGrabbers, usingFoundGrabbers = false;

    @Override
    public void init() {

        try {
            srvFoundLeft = hardwareMap.servo.get("srvFoundLeft");
            srvFoundRight = hardwareMap.servo.get("srvFoundRight");
        }catch (Exception e){}
        r = Robot.getInstance();
        r.initialize(this);
    }

    @Override
    public void loop() {

        if(gamepad1.a && !OSFoundGrabbers){
            OSFoundGrabbers = true;
            usingFoundGrabbers = !usingFoundGrabbers;
        } else if(!gamepad1.a) OSFoundGrabbers = false;

        if(usingFoundGrabbers){
            r.setServoPosition("srvFoundLeft", .85);
            r.setServoPosition("srvFoundRight", .85);
        } else {
            r.setServoPosition("srvFoundLeft", .1);
            r.setServoPosition("srvFoundRight", .1);
        }

        telemetry.addData("USING FOUNDATION GRABBERS: ", usingFoundGrabbers);

    }
}
