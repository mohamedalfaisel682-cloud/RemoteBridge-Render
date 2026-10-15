FROM golang:1.22-alpine AS builder

WORKDIR /app

# نسخ ملفات تعريف المشروع من مجلد server
COPY server/go.mod ./
RUN go mod download

# نسخ كود الخادم
COPY server/main.go ./

# بناء ملف السيرفر التنفيذي
RUN CGO_ENABLED=0 GOOS=linux go build -o /server main.go

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /server /app/server

EXPOSE 8080
CMD ["/server"]
