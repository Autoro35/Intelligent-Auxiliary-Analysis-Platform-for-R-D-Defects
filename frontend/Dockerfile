# ============================================================
# 前端镜像：多阶段构建
#   阶段一 node 构建静态资源，阶段二 nginx 托管并反向代理 /api
# ============================================================

# ---- 构建阶段 ----
FROM node:20-alpine AS build

WORKDIR /build

# 用 ci 保证与 package-lock.json 完全一致；先装依赖，源码变动时可复用缓存
COPY package.json package-lock.json ./
RUN npm ci --no-fund --no-audit

COPY . .
RUN npm run build

# ---- 运行阶段 ----
FROM nginx:1.27-alpine

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ > /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
