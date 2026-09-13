FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html
COPY . .
RUN rm -f ./Dockerfile /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 80
