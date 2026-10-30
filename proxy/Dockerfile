# Nginx reverse proxy with the config baked in (not bind-mounted), so the
# running stack does not depend on files in the Jenkins workspace.
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
