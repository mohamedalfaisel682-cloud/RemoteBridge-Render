FROM golang:1.22-alpine AS builder

WORKDIR /app

# نسخ ملفات المشروع
COPY go.mod ./

# تثبيت مكتبة websocket صراحة
RUN go get github.com/gorilla/websocket

# نسخ كود السيرفر
COPY main.go ./

# بناء السيرفر التنفيذي
RUN CGO_ENABLED=0 GOOS=linux go build -o /server main.go

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /server /app/server

EXPOSE 8080
CMD ["/server"]
