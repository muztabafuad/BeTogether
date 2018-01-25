package fr.inria.yifan.mysensor;

import java.io.Serializable;

/**
 * This class stores global configuration parameters for the application.
 */

class Configuration implements Serializable {

    // Parameters for audio sound signal sampling
    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int SAMPLE_DELAY_IN_MS = 100;


    // Permission request indicator code
    static final int PERMS_REQUEST_RECORD = 1000;
    static final int PERMS_REQUEST_STORAGE = 1001;
}
