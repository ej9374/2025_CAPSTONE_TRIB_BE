package triB.triB.auth.service;

public class MailTemplate {
    public static String signupCode(String authCode){
        return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head>
              <meta charset="UTF-8">
              <meta name="x-apple-disable-message-reformatting">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>TriB 회원가입 이메일 인증</title>
            </head>
            <body style="margin:0; padding:0; background-color:#f6f8fb;">
              <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="background-color:#f6f8fb;">
                <tr>
                  <td align="center" style="padding:24px;">
                    <table role="presentation" cellpadding="0" cellspacing="0" width="600" style="max-width:600px; width:100%%; background:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 16px rgba(0,0,0,0.06);">
                      
                      <!-- 헤더 -->
                      <tr>
                        <td style="background:#337196; padding:24px 28px;">
                          <table width="100%%" cellpadding="0" cellspacing="0" role="presentation">
                            <tr>
                              <td align="left" style="font-family:Arial, Helvetica, sans-serif; color:#ffffff; font-size:20px; font-weight:700;">
                                TriB
                              </td>
                              <td align="right" style="font-family:Arial, Helvetica, sans-serif; color:#ffffff; font-size:12px;">
                                회원가입 인증
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
            
                      <!-- 본문 -->
                      <tr>
                        <td style="padding:32px 28px 8px 28px; font-family:Arial, Helvetica, sans-serif; color:#111827;">
                          <h1 style="margin:0 0 10px 0; font-size:20px; line-height:1.4;">이메일 인증 번호 안내</h1>
                          <p style="margin:0; font-size:14px; line-height:1.7; color:black;">
                            안녕하세요. TriB 가입을 위한 인증 번호를 발급해 드립니다.<br>
                            아래의 인증 번호를 입력해 주세요.
                          </p>
                        </td>
                      </tr>
            
                      <!-- 인증 코드 박스 -->
                      <tr>
                        <td align="center" style="padding:22px 28px 8px 28px;">
                          <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="border-collapse:separate;">
                            <tr>
                              <td align="center" style="
                                  border:1px solid #e5e7eb;
                                  background:#f9fafb;
                                  border-radius:10px;
                                  padding:18px 16px;
                                  font-family:Arial, Helvetica, sans-serif;
                                  font-size:28px;
                                  letter-spacing:6px;
                                  font-weight:700;
                                  color:#111827;">
                                %s
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
            
                      <!-- 안내 문구 -->
                      <tr>
                        <td style="padding:8px 28px 24px 28px; font-family:Arial, Helvetica, sans-serif; color:#4b5563; font-size:12px; line-height:1.8;">
                          <ul style="margin:8px 0 0 18px; padding:0;">
                            <li>인증 번호는 요청한 사용자 본인만 사용해 주세요.</li>
                            <li>오입력을 방지하기 위해 복사/붙여넣기를 권장드립니다.</li>
                          </ul>
                        </td>
                      </tr>
            
                      <!-- 구분선 -->
                      <tr>
                        <td style="padding:0 28px;">
                          <hr style="border:none; border-top:1px solid #e5e7eb; margin:0;">
                        </td>
                      </tr>
            
                      <!-- 푸터 -->
                      <tr>
                        <td style="padding:18px 28px 24px 28px; font-family:Arial, Helvetica, sans-serif; color:#6b7280; font-size:12px; line-height:1.7;">
                          본 메일은 발신 전용입니다. 회신하지 말아 주세요.<br>
                          © %s TriB. All rights reserved.
                        </td>
                      </tr>
            
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """, authCode, java.time.Year.now());
    }
    public static String newPassword(String password){
        return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head>
              <meta charset="UTF-8">
              <meta name="x-apple-disable-message-reformatting">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>TriB 임시 비밀번호 생성</title>
            </head>
            <body style="margin:0; padding:0; background-color:#f6f8fb;">
              <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="background-color:#f6f8fb;">
                <tr>
                  <td align="center" style="padding:24px;">
                    <table role="presentation" cellpadding="0" cellspacing="0" width="600" style="max-width:600px; width:100%%; background:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 16px rgba(0,0,0,0.06);">
                      
                      <!-- 헤더 -->
                      <tr>
                        <td style="background:#337196; padding:24px 28px;">
                          <table width="100%%" cellpadding="0" cellspacing="0" role="presentation">
                            <tr>
                              <td align="left" style="font-family:Arial, Helvetica, sans-serif; color:#ffffff; font-size:20px; font-weight:700;">
                                TriB
                              </td>
                              <td align="right" style="font-family:Arial, Helvetica, sans-serif; color:#ffffff; font-size:12px;">
                                임시 비밀번호
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
            
                      <!-- 본문 -->
                      <tr>
                        <td style="padding:32px 28px 8px 28px; font-family:Arial, Helvetica, sans-serif; color:#111827;">
                          <h1 style="margin:0 0 10px 0; font-size:20px; line-height:1.4;">이메일 인증 번호 안내</h1>
                          <p style="margin:0; font-size:14px; line-height:1.7; color:black;">
                            안녕하세요. TriB 로그인을 위한 임시 비밀번호를 발급해 드립니다.<br>
                            아래의 비밀번호로 로그인 후, 비밀번호 변경을 해주세요.
                          </p>
                        </td>
                      </tr>
            
                      <!-- 임시 비밀번호 박스 -->
                      <tr>
                        <td align="center" style="padding:22px 28px 8px 28px;">
                          <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="border-collapse:separate;">
                            <tr>
                              <td align="center" style="
                                  border:1px solid #e5e7eb;
                                  background:#f9fafb;
                                  border-radius:10px;
                                  padding:18px 16px;
                                  font-family:Arial, Helvetica, sans-serif;
                                  font-size:28px;
                                  letter-spacing:6px;
                                  font-weight:700;
                                  color:#111827;">
                                %s
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
            
                      <!-- 안내 문구 -->
                      <tr>
                        <td style="padding:8px 28px 24px 28px; font-family:Arial, Helvetica, sans-serif; color:#4b5563; font-size:12px; line-height:1.8;">
                          <ul style="margin:8px 0 0 18px; padding:0;">
                            <li>임시 비밀번호는 요청한 사용자 본인만 사용해 주세요.</li>
                            <li>오입력을 방지하기 위해 복사/붙여넣기를 권장드립니다.</li>
                            <li>로그인 후 보안을 위해 즉시 비밀번호를 변경해주세요.</li>
                          </ul>
                        </td>
                      </tr>
            
                      <!-- 구분선 -->
                      <tr>
                        <td style="padding:0 28px;">
                          <hr style="border:none; border-top:1px solid #e5e7eb; margin:0;">
                        </td>
                      </tr>
            
                      <!-- 푸터 -->
                      <tr>
                        <td style="padding:18px 28px 24px 28px; font-family:Arial, Helvetica, sans-serif; color:#6b7280; font-size:12px; line-height:1.7;">
                          본 메일은 발신 전용입니다. 회신하지 말아 주세요.<br>
                          © %s TriB. All rights reserved.
                        </td>
                      </tr>
            
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """, password, java.time.Year.now());
    }


}
