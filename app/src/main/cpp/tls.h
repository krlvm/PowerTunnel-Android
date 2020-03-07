

#ifndef TUN2HTTP_TLS_H
#define TUN2HTTP_TLS_H

#include <stdint.h>

void parse_tls_header(const char *data, size_t data_len, char *hostname);

#endif //TUN2HTTP_TLS_H
