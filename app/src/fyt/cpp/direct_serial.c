#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <termios.h>
#include <unistd.h>

static void fail(JNIEnv *env, const char *operation, int error) {
    char message[256];
    snprintf(message, sizeof(message), "%s: %s (errno %d); vehicle unconfirmed",
             operation, strerror(error), error);
    jclass type = (*env)->FindClass(env, "java/io/IOException");
    if (type) (*env)->ThrowNew(env, type, message);
}

JNIEXPORT jint JNICALL
Java_com_cabin_hondacustom_DirectXpSerial_writeOnce(JNIEnv *env, jclass cls,
                                                  jstring path, jint baud, jbyteArray packet) {
    (void) cls;
    speed_t speed;
    switch (baud) {
        case 9600: speed = B9600; break;
        case 19200: speed = B19200; break;
        case 38400: speed = B38400; break;
        case 57600: speed = B57600; break;
        case 115200: speed = B115200; break;
        default: fail(env, "baud", EINVAL); return -1;
    }
    if (!path || !packet) { fail(env, "arguments", EINVAL); return -1; }
    jsize count = (*env)->GetArrayLength(env, packet);
    if (count < 3 || count > 66) { fail(env, "frame length", EINVAL); return -1; }
    jbyte data[66];
    (*env)->GetByteArrayRegion(env, packet, 0, count, data);
    if ((*env)->ExceptionCheck(env)) return -1;
    const char *name = (*env)->GetStringUTFChars(env, path, NULL);
    if (!name) return -1;
    // No create, shell, root, permission changes, baud changes, reads or fallback.
    int fd = open(name, O_WRONLY | O_NOCTTY | O_NONBLOCK | O_CLOEXEC | O_NOFOLLOW);
    int error = errno;
    (*env)->ReleaseStringUTFChars(env, path, name);
    if (fd < 0) { fail(env, "open (0 bytes written)", error); return -1; }
    struct stat info;
    struct termios config;
    if (fstat(fd, &info) < 0) {
        error = errno; close(fd); fail(env, "fstat (0 bytes written)", error); return -1;
    }
    if (!S_ISCHR(info.st_mode)) {
        close(fd); fail(env, "not a character device (0 bytes written)", ENODEV); return -1;
    }
    if (tcgetattr(fd, &config) < 0) {
        error = errno; close(fd); fail(env, "termios (0 bytes written)", error); return -1;
    }
    // Preserve the existing UART configuration. Refuse output transformations or
    // an incompatible rate/format instead of reconfiguring another client's port.
    if (cfgetospeed(&config) != speed || (config.c_cflag & CSIZE) != CS8 ||
        (config.c_cflag & (PARENB | CSTOPB | CRTSCTS)) ||
        (config.c_oflag & OPOST) || (config.c_iflag & (IXON | IXOFF))) {
        close(fd); fail(env, "expected existing raw output / baud / 8N1 (0 bytes written)", EINVAL); return -1;
    }
    // One nonblocking write. Never resend a partial or interrupted packet.
    ssize_t written = write(fd, data, (size_t) count);
    error = errno;
    close(fd);
    if (written < 0) { fail(env, "write (no automatic retry)", error); return -1; }
    return (jint) written;
}
