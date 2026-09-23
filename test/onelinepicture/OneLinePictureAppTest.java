package onelinepicture;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OneLinePictureAppTest
{
    @Test void applicationClassIsAvailable()
    {
        assertDoesNotThrow(() -> Class.forName("onelinepicture.OneLinePictureApp"));
    }
}
