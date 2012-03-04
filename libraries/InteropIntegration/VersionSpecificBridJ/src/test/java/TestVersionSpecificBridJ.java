import org.bridj.v0_7.BridJ;
import org.bridj.v0_7.Pointer;
import static org.bridj.v0_7.Pointer.*;
import org.junit.*;
import static org.junit.Assert.*;

public class TestVersionSpecificBridJ {
    @Test
    public void checkPointersWork() {
		assertEquals(2, (int)pointerToInts(1, 2, 3).get(1));
    }
}
