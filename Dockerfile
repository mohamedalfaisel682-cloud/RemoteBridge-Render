FROM golang:1.22-alpine AS builder

WORKDIR /app

# نسخ الكود والتعريفات
COPY go.mod main.go ./

# تنظيف وتحديث الاعتماديات وتوليد go.sum تلقائياً أثناء البناء
RUN go mod tidy

# بناء الملف التنفيذي
RUN CGO_ENABLED=0 GOOS=linux go build -o /server main.go

FROM alpine:3.20
WORKDIR /app
COPY --from=builder /server /app/server

EXPOSE 10000
CMD ["/server"]
