/**
 * The main class of this package is {@code ReaderManager}. Usually, the user
 * doesn't even have to worry about choosing a specific reader, so explicit
 * invocation of any methods in any one of the {@code Reader}s is discouraged
 * (this is a job for the {@code ReaderManager}. However, if it is necessary to
 * create a new reader for a format that {@code PULsE} currently doesn't
 * recognise, the programmer should extend one of the subclasses of the
 * {@code AbstractReader} and place it into this package (otherwise, the
 * {@code ReaderManager} won't know where the class is).
 */

package pulse.io.readers;