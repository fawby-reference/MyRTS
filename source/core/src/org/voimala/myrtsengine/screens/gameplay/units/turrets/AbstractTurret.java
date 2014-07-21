package org.voimala.myrtsengine.screens.gameplay.units.turrets;

import com.badlogic.gdx.math.Vector2;
import org.voimala.myrtsengine.screens.gameplay.ammunition.AbstractAmmunition;
import org.voimala.myrtsengine.screens.gameplay.units.AbstractUnit;
import org.voimala.myrtsengine.screens.gameplay.weapons.AbstractWeapon;
import org.voimala.myrtsengine.screens.gameplay.world.AbstractGameObject;
import org.voimala.utility.MathHelper;
import org.voimala.utility.RotationDirection;

import java.util.ArrayList;

public abstract class AbstractTurret extends AbstractGameObject {

    protected AbstractUnit owner;
    protected AbstractUnit target;
    protected AbstractWeapon weapon;

    protected double currentRotationVelocity = 0; /// deg/s
    protected double maxRotationVelocity = 0; /// deg/s
    protected double rotationAcceleration = 0; /// deg/s
    protected double rotationDeceleration = 0; /// deg/s
    protected RotationDirection currentRotationDirection;
    protected double steeringWheel = 0; // 1 = full clockwise, -1 = full counter-clockwise

    protected Vector2 relativePosition = new Vector2(0, 0); // Turrets position relative to the owner unit.

    protected long range = 100;

    public AbstractTurret(AbstractUnit owner, AbstractWeapon weapon) {
        this.owner = owner;
        this.weapon = weapon;
    }

    public void updateState(final float deltaTime) {
        updateTurretState(deltaTime);
        updateWeaponState();
    }

    private void updateTurretState(final float deltaTime) {
        updatePosition();
        updateRotation(deltaTime);
        checkTarget();
    }

    protected void updatePosition() {
        position.x = owner.getX() + relativePosition.x;
        position.y = owner.getY() + relativePosition.y;
    }

    private void updateWeaponState() {
        if (weapon != null) {
            weapon.updateState();
        }
    }

    private void updateRotation(final float deltaTime) {
        handlePhysicalRotation(deltaTime);
        handleLogicalRotation();
    }

    private void handlePhysicalRotation(final float deltaTime) {
        handlePhysicalRotationAcceleration(deltaTime);
        handlePhysicalRotationDeceleration(deltaTime);
        handlePhysicalRotationVelocity(deltaTime);
    }

    private void handlePhysicalRotationVelocity(float deltaTime) {
        // Change current rotation direction if rotation is stopped and steering wheel is turned.
        if (currentRotationVelocity == 0) {
            if (steeringWheel > 0) {
                currentRotationDirection = RotationDirection.CLOCKWISE;
            } else if (steeringWheel < 0) {
                currentRotationDirection = RotationDirection.COUNTERCLOCKWISE;
            }
        }

        // Handle current rotation direction
        if (currentRotationDirection == RotationDirection.CLOCKWISE) {
            rotate(-(float) (deltaTime * currentRotationVelocity));
        } else if (currentRotationDirection == RotationDirection.COUNTERCLOCKWISE) {
            rotate((float) (deltaTime * currentRotationVelocity));
        }
    }

    private void handlePhysicalRotationAcceleration(final float deltaTime) {
        if ((steeringWheel > 0 && currentRotationDirection == RotationDirection.CLOCKWISE)
                || (steeringWheel < 0 &&currentRotationDirection == RotationDirection.COUNTERCLOCKWISE)) {
            currentRotationVelocity += rotationAcceleration * deltaTime;

            if (currentRotationVelocity > maxRotationVelocity) {
                currentRotationVelocity = maxRotationVelocity;
            }
        }
    }

    private void handlePhysicalRotationDeceleration(final float deltaTime) {
        if (steeringWheel == 0
                || (steeringWheel > 0 && currentRotationDirection != RotationDirection.CLOCKWISE)
                || (steeringWheel < 0 && currentRotationDirection != RotationDirection.COUNTERCLOCKWISE)) {
            currentRotationVelocity -= rotationDeceleration * deltaTime;

            if (currentRotationVelocity < 0) {
                currentRotationVelocity = 0;
            }
        }
    }

    private void handleLogicalRotation() {
        if (hasTarget()) {
            rotateTowardsTarget();
        } else {
            rotateTowardsOwnerUnit();
        }
    }

    private void rotateTowardsTarget() {
        // TODO
    }

    private void rotateTowardsOwnerUnit() {
        // If turret is not looking at the same direction as the owner, set the correct rotation direction
        if (MathHelper.round(angle, 1)
                != MathHelper.round(owner.getAngle(), 1)) {
            RotationDirection targetRotationDirection = MathHelper.getFasterTurningDirection(getAngleInRadians(),
                    owner.getAngleInRadians());

            if (targetRotationDirection == RotationDirection.CLOCKWISE) {
                this.steeringWheel = 1;
            } else if (targetRotationDirection == RotationDirection.COUNTERCLOCKWISE) {
                this.steeringWheel = -1;
            }

            // Stop the rotation at the right time so that the rotation stops at the final angle

            // How much time does it take to stop rotation
            double timeToStopRotationInSeconds = currentRotationVelocity / rotationDeceleration;

            // Calculate distance between current angle and the next (final) angle
            double distanceBetweenCurrentAngleAndTargetAngle = MathHelper.getDistanceFromAngle1ToAngle2(
                    getAngleInRadians(),
                    owner.getAngleInRadians(),
                    targetRotationDirection);
            double distanceBetweenCurrentAngleAndTargetAngleDegree =
                    Math.toDegrees(distanceBetweenCurrentAngleAndTargetAngle);

            if (distanceBetweenCurrentAngleAndTargetAngleDegree <= currentRotationVelocity * timeToStopRotationInSeconds) {
                this.steeringWheel = 0;
            }
        } else {
            this.steeringWheel = 0;
        }
    }

    private void checkTarget() {
        if (hasTarget()) {
            if (isTargetInRange()) {
                AbstractAmmunition ammunition = weapon.shoot(getPosition(), angle);
                if (ammunition != null) {
                    owner.getWorldController().getAmmunitionContainer().add(ammunition);
                }
            } else {
                target = null; // Give up
            }
        } else {
            findNewTarget();
        }
    }

    private void findNewTarget() {
        // Can return null so the target is also be null if nothing is found.
        target = findClosestEnemyInRange();
    }

    protected AbstractUnit findClosestEnemyInRange() {
        // Find all units in range
        ArrayList<AbstractUnit> targetsInRange = new ArrayList<AbstractUnit>();
        for (AbstractUnit unit : owner.getWorldController().getUnitContainerAllUnits().getUnits()) {
            if (unit.getTeam() == owner.getTeam()) {
                continue;
            }

            if (MathHelper.getDistanceBetweenPoints(position.x, position.y, unit.getX(), unit.getY()) <= range) {
                targetsInRange.add(unit);
            }
        }

        // Find the closest one

        AbstractUnit currentClosestTarget = null;
        if (!targetsInRange.isEmpty()) {
            currentClosestTarget = targetsInRange.get(0);
        }

        for (AbstractUnit unit : targetsInRange) {
            if (MathHelper.getDistanceBetweenPoints(
                    position.x,
                    position.y,
                    unit.getX(),
                    unit.getY()) <
                    MathHelper.getDistanceBetweenPoints(
                            position.x,
                            position.y,
                            currentClosestTarget.getX(),
                            currentClosestTarget.getY())) {
                currentClosestTarget = unit;
            }
        }

        return currentClosestTarget;
    }

    private boolean hasTarget() {
        return target != null;
    }

    @Override
    protected void initializeDimensions() {
        // TODO Can be left null.
    }

    @Override
    protected void initializeCollisionMask() {
        // TODO Can be left null.
    }

    @Override
    protected void initializeMovement() {
        // TODO Can be left null.
    }

    @Override
    protected void updateCollisionMask() {
        // TODO Can be left null.
    }

    @Override
    public boolean onCollision(Vector2 point) {
        return false;
    }

    public boolean isTargetInRange() {
        if (hasTarget()) {
            return MathHelper.getDistanceBetweenPoints(position.x, position.y, target.getX(), target.getY()) <= range;
        }

        return false;
    }

    public Vector2 getRelativePosition() {
        return relativePosition;
    }

    public void setRelativePosition(Vector2 relativePosition) {
        this.relativePosition = relativePosition;
    }
}