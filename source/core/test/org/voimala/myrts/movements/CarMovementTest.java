package org.voimala.myrts.movements;import com.badlogic.gdx.math.Vector2;import junit.framework.TestCase;import org.voimala.myrts.screens.gameplay.units.infantry.M4Unit;import java.util.HashMap;public class CarMovementTest extends TestCase {    public void testDeterministicMovement() throws Exception {        // Run simulation 1        M4Unit m4Unit = new M4Unit(null);        m4Unit.getMovement().setSinglePathPoint(new Vector2(150, 100));        HashMap<Long, String> tickAndHash = new HashMap<Long, String>();        long worldUpdateTick = 0;        while (!m4Unit.getMovement().hasReachedDestination()) {            m4Unit.getMovement().update(0.05f);            tickAndHash.put(worldUpdateTick, m4Unit.getMovement().getStateHash());            worldUpdateTick++;        }        // Run simulation again x times and make sure the result is exactly the same as in simulation 1        int repeatSimulationTimes = 10;        for (int i = 0; i < repeatSimulationTimes; i++) {            M4Unit m4Unit2 = new M4Unit(null);                m4Unit2.getMovement().setSinglePathPoint(new Vector2(150, 100));            long worldUpdateTick2 = 0;            while (!m4Unit2.getMovement().hasReachedDestination()) {                m4Unit2.getMovement().update(0.05f);                if (!m4Unit2.getMovement().getStateHash().equals(tickAndHash.get(worldUpdateTick2))) {                    System.out.println("TEST FAILED! WORLD UPDATE " + worldUpdateTick2 + " IS OUT OF SYNC! \""                            + m4Unit2.getMovement().getStateHash() + "\" IS NOT \"" + tickAndHash.get(worldUpdateTick2) + "\"");                    assertTrue(false); // Test has failed                }                worldUpdateTick2++;            }        }        assertTrue(true); // Test passed    }}