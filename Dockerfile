FROM php:8.3-fpm-alpine

# Install build dependencies and runtime libraries
RUN apk add --no-cache --virtual .build-deps \
        build-base \
        autoconf \
        sqlite-dev \
        libpng-dev \
        libzip-dev \
        oniguruma-dev \
        freetype-dev \
        libjpeg-turbo-dev \
        libwebp-dev \
    && apk add --no-cache \
        libpng \
        libzip \
        libwebp \
        libjpeg-turbo \
        freetype \
        oniguruma \
    && docker-php-ext-configure gd \
        --with-freetype \
        --with-jpeg \
        --with-webp \
    && docker-php-ext-install -j$(nproc) \
        pdo_sqlite \
        pdo_mysql \
        gd \
        zip \
        bcmath \
    && apk del .build-deps

# Install runtime dependencies
RUN apk add --no-cache \
        nginx \
        sqlite \
        nodejs \
        npm \
        supervisor \
        shadow \
        curl \
        net-tools

# Install Composer
COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

# Set working directory
WORKDIR /var/www/html

# Copy dependency files first for better layer caching
COPY composer.json composer.lock package*.json ./

# Install dependencies
RUN composer install --optimize-autoloader --no-scripts --no-interaction \
    && npm ci --only=production \
    && npm cache clean --force

# Copy application code
COPY . .

# Build assets and cleanup
RUN npm run build \
    && rm -rf node_modules \
    && npm cache clean --force

# Create .env if missing
RUN [ ! -f .env ] && cp .env.example .env || true

# Create directories with proper permissions
RUN mkdir -p \
        /var/www/html/storage/database \
        /var/www/html/storage/logs \
        /var/www/html/storage/framework/cache \
        /var/www/html/storage/framework/sessions \
        /var/www/html/storage/framework/views \
        /var/www/html/bootstrap/cache \
        /var/log/nginx \
        /var/log/php-fpm \
        /var/run/php-fpm \
        /run/nginx \
    && touch /var/www/html/storage/database/database.sqlite \
    && chown -R www-data:www-data \
        /var/www/html/storage \
        /var/www/html/bootstrap/cache \
        /var/log/nginx \
        /var/log/php-fpm \
        /var/run/php-fpm \
        /run/nginx \
    && chmod -R 775 \
        /var/www/html/storage \
        /var/www/html/bootstrap/cache

# Create Nginx configuration directly in Dockerfile to ensure it's correct
RUN echo 'server {' > /etc/nginx/http.d/default.conf && \
    echo '    listen 8000;' >> /etc/nginx/http.d/default.conf && \
    echo '    server_name _;' >> /etc/nginx/http.d/default.conf && \
    echo '    root /var/www/html/public;' >> /etc/nginx/http.d/default.conf && \
    echo '    index index.php index.html;' >> /etc/nginx/http.d/default.conf && \
    echo '' >> /etc/nginx/http.d/default.conf && \
    echo '    # Security headers' >> /etc/nginx/http.d/default.conf && \
    echo '    add_header X-Frame-Options "SAMEORIGIN" always;' >> /etc/nginx/http.d/default.conf && \
    echo '    add_header X-Content-Type-Options "nosniff" always;' >> /etc/nginx/http.d/default.conf && \
    echo '    add_header X-XSS-Protection "1; mode=block" always;' >> /etc/nginx/http.d/default.conf && \
    echo '' >> /etc/nginx/http.d/default.conf && \
    echo '    # Logging' >> /etc/nginx/http.d/default.conf && \
    echo '    access_log /var/log/nginx/access.log;' >> /etc/nginx/http.d/default.conf && \
    echo '    error_log /var/log/nginx/error.log warn;' >> /etc/nginx/http.d/default.conf && \
    echo '' >> /etc/nginx/http.d/default.conf && \
    echo '    # Handle Laravel routes' >> /etc/nginx/http.d/default.conf && \
    echo '    location / {' >> /etc/nginx/http.d/default.conf && \
    echo '        try_files $uri $uri/ /index.php?$query_string;' >> /etc/nginx/http.d/default.conf && \
    echo '    }' >> /etc/nginx/http.d/default.conf && \
    echo '' >> /etc/nginx/http.d/default.conf && \
    echo '    # Handle PHP files' >> /etc/nginx/http.d/default.conf && \
    echo '    location ~ \.php$ {' >> /etc/nginx/http.d/default.conf && \
    echo '        try_files $uri =404;' >> /etc/nginx/http.d/default.conf && \
    echo '        fastcgi_split_path_info ^(.+\.php)(/.+)$;' >> /etc/nginx/http.d/default.conf && \
    echo '        fastcgi_pass 127.0.0.1:9000;' >> /etc/nginx/http.d/default.conf && \
    echo '        fastcgi_index index.php;' >> /etc/nginx/http.d/default.conf && \
    echo '        fastcgi_param SCRIPT_FILENAME $realpath_root$fastcgi_script_name;' >> /etc/nginx/http.d/default.conf && \
    echo '        include fastcgi_params;' >> /etc/nginx/http.d/default.conf && \
    echo '        fastcgi_param PATH_INFO $fastcgi_path_info;' >> /etc/nginx/http.d/default.conf && \
    echo '        fastcgi_read_timeout 300;' >> /etc/nginx/http.d/default.conf && \
    echo '    }' >> /etc/nginx/http.d/default.conf && \
    echo '' >> /etc/nginx/http.d/default.conf && \
    echo '    # Security: Deny access to hidden files' >> /etc/nginx/http.d/default.conf && \
    echo '    location ~ /\. {' >> /etc/nginx/http.d/default.conf && \
    echo '        deny all;' >> /etc/nginx/http.d/default.conf && \
    echo '        access_log off;' >> /etc/nginx/http.d/default.conf && \
    echo '        log_not_found off;' >> /etc/nginx/http.d/default.conf && \
    echo '    }' >> /etc/nginx/http.d/default.conf && \
    echo '' >> /etc/nginx/http.d/default.conf && \
    echo '    # Cache static assets' >> /etc/nginx/http.d/default.conf && \
    echo '    location ~* \.(css|js|jpg|jpeg|png|gif|ico|svg|woff|woff2|ttf|eot)$ {' >> /etc/nginx/http.d/default.conf && \
    echo '        expires 1y;' >> /etc/nginx/http.d/default.conf && \
    echo '        add_header Cache-Control "public, immutable";' >> /etc/nginx/http.d/default.conf && \
    echo '        access_log off;' >> /etc/nginx/http.d/default.conf && \
    echo '    }' >> /etc/nginx/http.d/default.conf && \
    echo '}' >> /etc/nginx/http.d/default.conf

# Create PHP-FPM configuration directly in Dockerfile
RUN echo '[global]' > /usr/local/etc/php-fpm.d/www.conf && \
    echo 'error_log = /var/log/php-fpm/error.log' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'log_level = warning' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'daemonize = no' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '[www]' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'user = www-data' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'group = www-data' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '; TCP configuration' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'listen = 127.0.0.1:9000' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '; Process management' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'pm = dynamic' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'pm.max_children = 10' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'pm.start_servers = 3' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'pm.min_spare_servers = 2' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'pm.max_spare_servers = 5' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'pm.max_requests = 500' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '; Security' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'security.limit_extensions = .php' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo '; Environment' >> /usr/local/etc/php-fpm.d/www.conf && \
    echo 'clear_env = no' >> /usr/local/etc/php-fpm.d/www.conf

# Create Supervisord configuration directly in Dockerfile
RUN echo '[supervisord]' > /etc/supervisord.conf && \
    echo 'nodaemon=true' >> /etc/supervisord.conf && \
    echo 'user=root' >> /etc/supervisord.conf && \
    echo 'logfile=/var/log/supervisord.log' >> /etc/supervisord.conf && \
    echo 'pidfile=/var/run/supervisord.pid' >> /etc/supervisord.conf && \
    echo '' >> /etc/supervisord.conf && \
    echo '[program:nginx]' >> /etc/supervisord.conf && \
    echo 'command=nginx -g "daemon off;"' >> /etc/supervisord.conf && \
    echo 'autostart=true' >> /etc/supervisord.conf && \
    echo 'autorestart=true' >> /etc/supervisord.conf && \
    echo 'startsecs=1' >> /etc/supervisord.conf && \
    echo 'startretries=3' >> /etc/supervisord.conf && \
    echo 'stdout_logfile=/var/log/nginx/supervisor.log' >> /etc/supervisord.conf && \
    echo 'stderr_logfile=/var/log/nginx/supervisor.err' >> /etc/supervisord.conf && \
    echo 'user=root' >> /etc/supervisord.conf && \
    echo 'priority=10' >> /etc/supervisord.conf && \
    echo '' >> /etc/supervisord.conf && \
    echo '[program:php-fpm]' >> /etc/supervisord.conf && \
    echo 'command=php-fpm -F' >> /etc/supervisord.conf && \
    echo 'autostart=true' >> /etc/supervisord.conf && \
    echo 'autorestart=true' >> /etc/supervisord.conf && \
    echo 'startsecs=1' >> /etc/supervisord.conf && \
    echo 'startretries=3' >> /etc/supervisord.conf && \
    echo 'stdout_logfile=/var/log/php-fpm/supervisor.log' >> /etc/supervisord.conf && \
    echo 'stderr_logfile=/var/log/php-fpm/supervisor.err' >> /etc/supervisord.conf && \
    echo 'user=root' >> /etc/supervisord.conf && \
    echo 'priority=20' >> /etc/supervisord.conf

# Create entrypoint script directly in Dockerfile
RUN echo '#!/bin/sh' > /entrypoint.sh && \
    echo 'set -e' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo 'log() {' >> /entrypoint.sh && \
    echo '    echo "[$(date "+%Y-%m-%d %H:%M:%S")] $1"' >> /entrypoint.sh && \
    echo '}' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo 'log "Starting container initialization..."' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Set permissions' >> /entrypoint.sh && \
    echo 'log "Setting up permissions..."' >> /entrypoint.sh && \
    echo 'chown -R www-data:www-data /var/www/html/storage /var/www/html/bootstrap/cache' >> /entrypoint.sh && \
    echo 'chmod -R 775 /var/www/html/storage /var/www/html/bootstrap/cache' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Create SQLite database' >> /entrypoint.sh && \
    echo 'if [ ! -f /var/www/html/storage/database/database.sqlite ]; then' >> /entrypoint.sh && \
    echo '    log "Creating SQLite database..."' >> /entrypoint.sh && \
    echo '    touch /var/www/html/storage/database/database.sqlite' >> /entrypoint.sh && \
    echo 'fi' >> /entrypoint.sh && \
    echo 'chown www-data:www-data /var/www/html/storage/database/database.sqlite' >> /entrypoint.sh && \
    echo 'chmod 664 /var/www/html/storage/database/database.sqlite' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Create .env file' >> /entrypoint.sh && \
    echo 'if [ ! -f /var/www/html/.env ]; then' >> /entrypoint.sh && \
    echo '    log "Creating .env file..."' >> /entrypoint.sh && \
    echo '    cp /var/www/html/.env.example /var/www/html/.env' >> /entrypoint.sh && \
    echo '    chown www-data:www-data /var/www/html/.env' >> /entrypoint.sh && \
    echo 'fi' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Create log files' >> /entrypoint.sh && \
    echo 'log "Setting up log directories..."' >> /entrypoint.sh && \
    echo 'mkdir -p /var/log/nginx /var/log/php-fpm' >> /entrypoint.sh && \
    echo 'touch /var/log/nginx/access.log /var/log/nginx/error.log' >> /entrypoint.sh && \
    echo 'touch /var/log/php-fpm/error.log' >> /entrypoint.sh && \
    echo 'chown -R www-data:www-data /var/log/nginx /var/log/php-fpm' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Test configurations' >> /entrypoint.sh && \
    echo 'log "Testing Nginx configuration..."' >> /entrypoint.sh && \
    echo 'nginx -t' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo 'log "Testing PHP-FPM configuration..."' >> /entrypoint.sh && \
    echo 'php-fpm -t' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Laravel setup' >> /entrypoint.sh && \
    echo 'if [ -f /var/www/html/artisan ]; then' >> /entrypoint.sh && \
    echo '    log "Running Laravel setup..."' >> /entrypoint.sh && \
    echo '    if ! grep -q "APP_KEY=base64:" /var/www/html/.env; then' >> /entrypoint.sh && \
    echo '        log "Generating application key..."' >> /entrypoint.sh && \
    echo '        php artisan key:generate --force' >> /entrypoint.sh && \
    echo '    fi' >> /entrypoint.sh && \
    echo '    php artisan config:clear' >> /entrypoint.sh && \
    echo '    php artisan route:clear' >> /entrypoint.sh && \
    echo '    php artisan view:clear' >> /entrypoint.sh && \
    echo '    php artisan cache:clear' >> /entrypoint.sh && \
    echo 'fi' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo 'log "Container initialization completed!"' >> /entrypoint.sh && \
    echo 'exec "$@"' >> /entrypoint.sh && \
    chmod +x /entrypoint.sh

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/ || exit 1

# Use entrypoint to handle runtime setup
ENTRYPOINT ["/entrypoint.sh"]
CMD ["supervisord", "-c", "/etc/supervisord.conf"]