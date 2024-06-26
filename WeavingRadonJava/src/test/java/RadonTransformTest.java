import com.marginallyclever.weavingradon.core.LoomThread;
import com.marginallyclever.weavingradon.core.RadonTransform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.vecmath.Vector2d;
import java.awt.*;

public class RadonTransformTest {
    @Test
    public void testOneIntersection() {
        int radius = 100;
        int r = 0;
        int theta = 0;

        LoomThread thread = new LoomThread(
                new Vector2d(radius,0),
                new Vector2d(radius,radius*2),
                theta,
                r,
                Color.WHITE
        );
        RadonTransform rt = new RadonTransform(radius,thread);

        double c = rt.testIntersection(thread,theta,r);
        Assertions.assertEquals(radius,c);
        c = rt.testIntersection(thread,theta,r+1);
        Assertions.assertEquals(0,c);
        c = rt.testIntersection(thread,theta,r-1);
        Assertions.assertEquals(0,c);

        c = rt.testIntersection(thread,theta+90,r);
        Assertions.assertEquals(1,c);
        c = rt.testIntersection(thread,theta+90,r+1);
        Assertions.assertEquals(1,c);
        c = rt.testIntersection(thread,theta+90,r-1);
        Assertions.assertEquals(1,c);

        c = rt.testIntersection(thread,theta+45,r);
        assert(c>1 && c<radius);
    }
}
