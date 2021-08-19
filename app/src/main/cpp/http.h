/*
    This file is part of NetGuard.

    NetGuard is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2015-2017 by Marcel Bokhorst (M66B)
*/

#ifndef TUN2HTTP_HTTP_H
#define TUN2HTTP_HTTP_H

#include <stdint.h>

int get_header(const char *header, const char *data, size_t data_len, char *value);
int next_header(const char **data, size_t *len);
uint8_t *find_data(uint8_t *data, size_t data_len, char *value);

uint8_t *patch_http_url(uint8_t *data, size_t *data_len);

#endif //TUN2HTTP_TLS_H
