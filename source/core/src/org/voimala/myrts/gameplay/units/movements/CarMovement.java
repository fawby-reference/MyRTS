package org.voimala.myrts.gameplay.units.movements;

import com.badlogic.gdx.math.Vector2;
import org.voimala.myrts.gameplay.units.Unit;
import org.voimala.utility.MathHelper;

import java.util.ArrayList;

public class CarMovement extends Movement {
    private ArrayList<Vector2> pathPoints = new ArrayList<>();
    private double acceleratorPedal = 0; /// 0 = no acceleration, 1 = full acceleration.
    private boolean stopAtFinalPoint = true;

    public CarMovement(Unit ownerUnit) {
        super(ownerUnit);
    }

    public void update(final float deltaTime) {
        handleMovement(deltaTime);
        handleAI(deltaTime);
    }

    private void handleMovement(float deltaTime) {
        handleAcceleration(deltaTime);
        handleDeceleration(deltaTime);
        ownerUnit.moveX(Math.cos(ownerUnit.getAngleInRadians()) * currentVelocity * deltaTime);
        ownerUnit.moveY(Math.sin(ownerUnit.getAngleInRadians()) * currentVelocity * deltaTime);
    }

    private void handleAcceleration(final float deltaTime) {
        if (acceleratorPedal > 0) {
            currentVelocity += acceleration * deltaTime;

            if (currentVelocity >= maxVelocity) {
                currentVelocity = maxVelocity;
            }
        }
    }

    private void handleDeceleration(final float deltaTime) {
        if (acceleratorPedal == 0) {
            currentVelocity -= deceleration * deltaTime;

            if (currentVelocity < 0) {
                currentVelocity = 0;
            }
        }
    }

    private void handleAI(float deltaTime) {
        if(!pathPoints.isEmpty()) {
            drive(deltaTime);
        } else {
            stop(deltaTime);
        }
    }

    private void drive(final float deltaTime) {
        acceleratorPedal = 1;

        Vector2 nextPoint = pathPoints.get(0);
        driveTowardsPoint(deltaTime, nextPoint);
        stopAtFinalPoint(nextPoint);

        if (hasReachedPoint(nextPoint)) {
            pathPoints.remove(nextPoint);
        }
    }

    private void driveTowardsPoint(final float deltaTime, final Vector2 nextPoint) {
        rotateTowardsPoint(deltaTime, nextPoint);
    }

    private void stopAtFinalPoint(final Vector2 nextPoint) {
        if (stopAtFinalPoint && pathPoints.size() == 1) {
            // Release accelerator at the right time so that the car stops at the final point

            // How much time does it take for the car to stop
            double timeToStopInSeconds = currentVelocity / deceleration;

            // Calculate distance between current point and the next (final) point
            double distanceBetweenCurrentPointAndEndPoint = MathHelper.getDistanceBetweenPoints(
                    ownerUnit.getX(), ownerUnit.getY(), nextPoint.x, nextPoint.y);

            if (distanceBetweenCurrentPointAndEndPoint <= currentVelocity * timeToStopInSeconds) {
                acceleratorPedal = 0;
            }
        }
    }

    private void rotateTowardsPoint(final float deltaTime, final Vector2 point) {
        double angleBetweenUnitAndPointInRadians = MathHelper.getAngleBetweenPointsInRadians(
                ownerUnit.getX(),
                ownerUnit.getY(),
                point.x,
                point.y);

        // If unit is not looking at the point, rotate it
        if (MathHelper.round(ownerUnit.getAngleInRadians(), 4)
                != MathHelper.round(angleBetweenUnitAndPointInRadians, 4)) {
            int rotationDirection = MathHelper.getFasterTurningDirection(ownerUnit.getAngleInRadians(),
                    angleBetweenUnitAndPointInRadians);

            if (rotationDirection == 1) {
                ownerUnit.rotate(-(float) (deltaTime * maxRotationVelocity));
            } else if (rotationDirection == 2) {
                ownerUnit.rotate((float) (deltaTime * maxRotationVelocity));
            }
        }
    }

    private void stop(final float deltaTime) {
        acceleratorPedal = 0;
    }

    public void addPathPoint(final Vector2 point) {
        pathPoints.add(point);
    }

    public ArrayList<Vector2> getPathPoints() {
        return pathPoints;
    }
}
