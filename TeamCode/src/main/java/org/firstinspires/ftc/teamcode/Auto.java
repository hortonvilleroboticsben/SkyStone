package org.firstinspires.ftc.teamcode;

import android.os.Environment;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

@Autonomous(name="AutoTest")
public class Auto extends OpMode {
    StateMachine drive = new StateMachine();
    StateMachine vision = new StateMachine();
    StateMachine lift = new StateMachine();
    Robot r = Robot.getInstance();

    File rFolder;
    ArrayList<String> fList = new ArrayList<>();
    boolean confirmOS = false, selOS = false;
    int fIndex = 0;

    String allianceColor = "blue"; //default
    String returnPath = "wall"; //default
    String apFoundationOrientation = "|"; //default
    boolean loadingSideStart = true; //default
    boolean apMoveFoundation = true; //default
    long wait = 0; //default
    int skyStones = 2; //default
    JSONObject settings;

    double leftOpen = .3, leftClosed = .18;
    double rightOpen = .19, rightClosed = .4;
    double rotatorOpen = .2, rotatorClosed = .8;
    double foundDown = .2, foundUp = .8;

    int[] vsData = null;

    @Override
    public void init(){
        r.initialize(this);

        rFolder = new File(Environment.getExternalStorageDirectory() + "/JSONConfigs");
        if (!rFolder.exists())
            rFolder.mkdir();
        for (File f : rFolder.listFiles()) {
            if (f.getName().substring(f.getName().length() - 5).equals(".json"))
                fList.add(f.getName());
        }
    }

    @Override
    public void init_loop(){
        if (!confirmOS) {
            if (fList.size() != 0) {
                telemetry.addData("Please select a file", fList.get(fIndex));

                if (gamepad1.dpad_down && !selOS) {
                    fIndex++;
                    selOS = true;
                }
                if (gamepad1.dpad_up && !selOS) {
                    fIndex--;
                    selOS = true;
                }
                if (!gamepad1.dpad_up && !gamepad1.dpad_down) selOS = false;

                fIndex = fIndex > fList.size() - 1 ? fList.size() - 1 : fIndex < 0 ? 0 : fIndex;

                if (gamepad1.a && !gamepad1.start && !gamepad2.start){
                    confirmOS = true;
                    telemetry.addData("File Selected",""+fList.get(fIndex));
                }
            } else {
                telemetry.addData("ERROR", "No files available.");
            }
        } else {
            File rPath = new File(rFolder, fList.get(fIndex));
            try {
                InputStream fin = new FileInputStream(rPath);
                byte[] raw_inp = new byte[fin.available()];
                fin.read(raw_inp);
                String om = new String(raw_inp);
                settings = new JSONObject(om);
                allianceColor = settings.getString("alliance");
                loadingSideStart = settings.getBoolean("loadingSideStart");
                wait = settings.getLong("pauseTime");
                returnPath = settings.getString("returnPath");
                skyStones = settings.getInt("skyStones");
                apMoveFoundation = settings.getBoolean("apMoveFoundation");
                apFoundationOrientation = settings.getString("apFoundationOrientation");
                telemetry.addData("Alliance",  allianceColor);
                telemetry.addData("Side",loadingSideStart?"Loading Side":"Building Side");
                telemetry.addData("Wait Time", wait);
                telemetry.addData("Driving Path", returnPath);
                telemetry.addData("SkyStones",skyStones==3?"Both":skyStones==2?"Second":"First");
                telemetry.addData("Foundation", apMoveFoundation?"Yes":"No");
            } catch (Exception e) {
                e.printStackTrace();
                telemetry.addData("TRY CATCH ERROR", e.getCause());
            }

        }
    }

    @Override
    public void loop(){
        drive.initializeMachine();
        vision.initializeMachine();
        lift.initializeMachine();

        int[] temp = vision.getVisionData();
        vsData = temp == null ? vsData : temp;
        final int placement = allianceColor.equals("red") ? (5-vsData[0])%3 + 1 : vsData[0];
        telemetry.addData("Placement: ", placement + "");

        vision.SetFlag(drive, "Vision Done");
        vision.SetFlag(lift, "Vision Done");

        drive.pause(wait);

        drive.WaitForFlag("Vision Done");

        lift.WaitForFlag("Vision Done");
        lift.runToTarget("mtrLift", -200,0.5);
        lift.setServoPosition("srvClampLeft", leftOpen);
        lift.setServoPosition("srvClampRight", rightOpen);
        lift.setServoPower("srvRotator", rotatorOpen);

        drive.translate(90,0.5,26);
        drive.rotate(-90,0.5);

        switch (placement) {
            case 1:
                drive.translate(-140, 0.5, 8);
                break;
            case 2:
                drive.translate(160, 0.5, 8);
                break;
            case 3:
                drive.translate(110, 0.5, 12);
                break;
        }

        drive.SetFlag(lift, "In Position");

        lift.WaitForFlag("In Position");

        lift.runToTarget("mtrLift",200,0.5);
        lift.setServoPosition("srvClampLeft", leftClosed);
        lift.setServoPosition("srvClampRight", rightClosed);

        lift.SetFlag(drive, "Grabbed");

        drive.WaitForFlag("Grabbed");

        drive.translate(0,0.5,5);

        switch(placement){//Move to skyStone pos 1 even if we are at pos 2 || 3
            case 2:
                drive.translate(-87,0.5,8);
                break;
            case 3:
                drive.translate(-87,0.5,16);
                break;
        }

        if(returnPath.equals("wall")) {
            drive.translate(0, 0.5, 22);
        }
        //Drive to Under Bridge
        drive.translate(-87,0.5,50);

        if(!apMoveFoundation) {
            //If we are moving foundation 73
            drive.SetFlag(lift,"Raise");
            drive.translate(-87, 0.5, 23);

            lift.WaitForFlag("Raise");
            lift.runToTarget("mtrLift",-400,0.5);

            if(returnPath.equals("wall")){
               drive.translate(180,0.5,24);
            }
            drive.translate(180,0.5,2);
            drive.SetFlag(lift,"Place");

            lift.WaitForFlag("Place");
            lift.setServoPosition("srvClampLeft",leftOpen);
            lift.setServoPosition("srvClampRight",rightOpen);
            lift.SetFlag(drive,"Placed");

            drive.WaitForFlag("Placed");
            drive.translate(0,0.5,5);
            drive.rotate(180,0.5);
            drive.SetFlag(lift,"DropLift");

            lift.WaitForFlag("DropLift");
            lift.runToTarget("mtrLift",400,0.5);

            drive.translate(0,0.5,5);
            //Close Foundation Grabbers
            drive.translate(180,0.5,25);
            //Open Foundation Grabbers
            drive.translate(-90,0.5,25);
            if(returnPath.equals("wall")){
                drive.translate(-90,0.5,10);
            } else {
                drive.translate(-30,0.5,50);
            }

        }
    }
}
