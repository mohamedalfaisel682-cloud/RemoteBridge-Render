FROM golang:1.22-alpine AS builder

WORKDIR /app

# نسخ ملفات الوحدات وتحميلها
COPY go.mod ./
RUN go mod download

# نسخ ملف السورس
COPY main.go ./

# بناء السيرفر
RUN CGO_ENABLED=0 GOOS=linux go build -o /server main.go

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /server /app/server

EXPOSE 8080
CMD ["/server"]
