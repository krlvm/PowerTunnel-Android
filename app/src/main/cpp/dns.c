
#include "tun2http.h"

int32_t get_qname(const uint8_t *data, const size_t datalen, uint16_t off, char *qname) {
    *qname = 0;

    uint16_t c = 0;
    uint8_t noff = 0;
    uint16_t ptr = off;
    uint8_t len = *(data + ptr);
    while (len) {
        if (len & 0xC0) {
            ptr = (uint16_t) ((len & 0x3F) * 256 + *(data + ptr + 1));
            len = *(data + ptr);
            log_android(ANDROID_LOG_DEBUG, "DNS qname compression ptr %d len %d", ptr, len);
            if (!c) {
                c = 1;
                off += 2;
            }
        }
        else if (ptr + 1 + len <= datalen && noff + len <= DNS_QNAME_MAX) {
            memcpy(qname + noff, data + ptr + 1, len);
            *(qname + noff + len) = '.';
            noff += (len + 1);

            ptr += (len + 1);
            len = *(data + ptr);
        }
        else
            break;
    }
    ptr++;

    if (len > 0 || noff == 0) {
        log_android(ANDROID_LOG_ERROR, "DNS qname invalid len %d noff %d", len, noff);
        return -1;
    }

    *(qname + noff - 1) = 0;
    log_android(ANDROID_LOG_DEBUG, "qname %s", qname);

    return (c ? off : ptr);
}

void parse_dns_response(const struct arguments *args, const struct udp_session *u,
                        const uint8_t *data, size_t *datalen) {
    if (*datalen < sizeof(struct dns_header) + 1) {
        log_android(ANDROID_LOG_WARN, "DNS response length %d", *datalen);
        return;
    }

    // Check if standard DNS query
    // TODO multiple qnames
    struct dns_header *dns = (struct dns_header *) data;
    int qcount = ntohs(dns->q_count);
    int acount = ntohs(dns->ans_count);
    if (dns->qr == 1 && dns->opcode == 0 && qcount > 0 && acount > 0) {
        log_android(ANDROID_LOG_DEBUG, "DNS response qcount %d acount %d", qcount, acount);
        if (qcount > 1)
            log_android(ANDROID_LOG_WARN, "DNS response qcount %d acount %d", qcount, acount);

        // http://tools.ietf.org/html/rfc1035
        char name[DNS_QNAME_MAX + 1];
        int32_t off = sizeof(struct dns_header);

        uint16_t qtype;
        uint16_t qclass;
        char qname[DNS_QNAME_MAX + 1];

        for (int q = 0; q < 1; q++) {
            off = get_qname(data, *datalen, (uint16_t) off, name);
            if (off > 0 && off + 4 <= *datalen) {
                // TODO multiple qnames?
                if (q == 0) {
                    strcpy(qname, name);
                    qtype = ntohs(*((uint16_t *) (data + off)));
                    qclass = ntohs(*((uint16_t *) (data + off + 2)));
                    log_android(ANDROID_LOG_DEBUG,
                                "DNS question %d qtype %d qclass %d qname %s",
                                q, qtype, qclass, qname);
                }
                off += 4;
            }
            else {
                log_android(ANDROID_LOG_WARN,
                            "DNS response Q invalid off %d datalen %d", off, *datalen);
                return;
            }
        }

        int32_t aoff = off;
        for (int a = 0; a < acount; a++) {
            off = get_qname(data, *datalen, (uint16_t) off, name);
            if (off > 0 && off + 10 <= *datalen) {
                uint16_t qtype = ntohs(*((uint16_t *) (data + off)));
                uint16_t qclass = ntohs(*((uint16_t *) (data + off + 2)));
                uint32_t ttl = ntohl(*((uint32_t *) (data + off + 4)));
                uint16_t rdlength = ntohs(*((uint16_t *) (data + off + 8)));
                off += 10;

                if (off + rdlength <= *datalen) {
                    if (qclass == DNS_QCLASS_IN &&
                        (qtype == DNS_QTYPE_A || qtype == DNS_QTYPE_AAAA)) {

                        char rd[INET6_ADDRSTRLEN + 1];
                        if (qtype == DNS_QTYPE_A)
                            inet_ntop(AF_INET, data + off, rd, sizeof(rd));
                        else if (qclass == DNS_QCLASS_IN && qtype == DNS_QTYPE_AAAA)
                            inet_ntop(AF_INET6, data + off, rd, sizeof(rd));
                    }
                    else
                        log_android(ANDROID_LOG_DEBUG,
                                    "DNS answer %d qname %s qclass %d qtype %d ttl %d length %d",
                                    a, name, qclass, qtype, ttl, rdlength);

                    off += rdlength;
                }
                else {
                    log_android(ANDROID_LOG_WARN,
                                "DNS response A invalid off %d rdlength %d datalen %d",
                                off, rdlength, *datalen);
                    return;
                }
            }
            else {
                log_android(ANDROID_LOG_WARN,
                            "DNS response A invalid off %d datalen %d", off, *datalen);
                return;
            }
        }
    }
    else if (acount > 0)
        log_android(ANDROID_LOG_WARN,
                    "DNS response qr %d opcode %d qcount %d acount %d",
                    dns->qr, dns->opcode, qcount, acount);
}

int get_dns_query(const struct arguments *args, const struct udp_session *u,
                  const uint8_t *data, const size_t datalen,
                  uint16_t *qtype, uint16_t *qclass, char *qname) {
    if (datalen < sizeof(struct dns_header) + 1) {
        log_android(ANDROID_LOG_WARN, "DNS query length %d", datalen);
        return -1;
    }

    // Check if standard DNS query
    // TODO multiple qnames
    const struct dns_header *dns = (struct dns_header *) data;
    int qcount = ntohs(dns->q_count);
    if (dns->qr == 0 && dns->opcode == 0 && qcount > 0) {
        if (qcount > 1)
            log_android(ANDROID_LOG_WARN, "DNS query qcount %d", qcount);

        // http://tools.ietf.org/html/rfc1035
        int off = get_qname(data, datalen, sizeof(struct dns_header), qname);
        if (off > 0 && off + 4 == datalen) {
            *qtype = ntohs(*((uint16_t *) (data + off)));
            *qclass = ntohs(*((uint16_t *) (data + off + 2)));
            return 0;
        }
        else
            log_android(ANDROID_LOG_WARN, "DNS query invalid off %d datalen %d", off, datalen);
    }

    return -1;
}

int check_domain(const struct arguments *args, const struct udp_session *u,
                 const uint8_t *data, const size_t datalen,
                 uint16_t qclass, uint16_t qtype, const char *name) {
    return 0;
}
