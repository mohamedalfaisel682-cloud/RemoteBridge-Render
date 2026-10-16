FROM golang:1.22-alpine AS builder

WORKDIR /app

# نسخ اعتماديات Go وتنزيل المكتبات
COPY go.mod ./
RUN go mod download

# نسخ ملف الكود الرئيسي
COPY main.go ./

# بناء الملف التنفيذي
RUN CGO_ENABLED=0 GOOS=linux go build -o /server main.go

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /server /app/server

# تعريض المنفذ 10000 المتوافق مع بيئة Render
EXPOSE 10000
CMD ["/server"]
