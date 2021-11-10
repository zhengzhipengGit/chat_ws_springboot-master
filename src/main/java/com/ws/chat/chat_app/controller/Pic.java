package com.ws.chat.chat_app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

@Controller
public class Pic {
	private final Font mFont =
			new Font("Arial Black", Font.PLAIN, 16);
	private final int IMG_WIDTH = 100;
	private final int IMG_HEIGTH = 18;
	private Color getRandColor(int fc,int bc)
	{
		Random random = new Random();
		if(fc > 255) fc = 255;
		if(bc > 255) bc=255;
		int r = fc + random.nextInt(bc - fc);
		int g = fc + random.nextInt(bc - fc);
		int b = fc + random.nextInt(bc - fc);
		return new Color(r , g , b);
	}
@RequestMapping(value="/authImg")
public void getpic(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	response.setHeader("Pragma","No-cache");
	response.setHeader("Cache-Control","no-cache");
	response.setDateHeader("Expires", 0);
	response.setContentType("image/jpeg");
	BufferedImage image = new BufferedImage
			(IMG_WIDTH , IMG_HEIGTH , BufferedImage.TYPE_INT_RGB);
	Graphics g = image.getGraphics();
	Random random = new Random();
	g.setColor(getRandColor(200 , 250));
	g.fillRect(1, 1, IMG_WIDTH - 1, IMG_HEIGTH - 1);
	g.setColor(new Color(102 , 102 , 102));
	g.drawRect(0, 0, IMG_WIDTH - 1, IMG_HEIGTH - 1);
	g.setColor(getRandColor(160,200));
	for (int i = 0 ; i < 30 ; i++)
	{
		int x = random.nextInt(IMG_WIDTH - 1);
		int y = random.nextInt(IMG_HEIGTH - 1);
		int xl = random.nextInt(6) + 1;
		int yl = random.nextInt(12) + 1;
		g.drawLine(x , y , x + xl , y + yl);
	}
	g.setColor(getRandColor(160,200));
	for (int i = 0 ; i < 30 ; i++)
	{
		int x = random.nextInt(IMG_WIDTH - 1);
		int y = random.nextInt(IMG_HEIGTH - 1);
		int xl = random.nextInt(12) + 1;
		int yl = random.nextInt(6) + 1;
		g.drawLine(x , y , x - xl , y - yl);
	}
	g.setFont(mFont);
	String sRand = "";
	for (int i = 0 ; i < 4 ; i++)
	{
		String tmp = getRandomChar();
		sRand += tmp;
		g.setColor(new Color(20 + random.nextInt(110)
				,20 + random.nextInt(110)
				,20 + random.nextInt(110)));
		g.drawString(tmp , 15 * i + 10,15);
	}
	HttpSession session = request.getSession(true);
	session.setAttribute("rand" , sRand);
//		System.out.println("写入session"+sRand);
	g.dispose();
	ImageIO.write(image, "JPEG", response.getOutputStream());
}
	private String getRandomChar()
	{
		int rand = (int)Math.round(Math.random() * 2);
		long itmp = 0;
		char ctmp = '\u0000';
		switch (rand)
		{
			case 1:
				itmp = Math.round(Math.random() * 25 + 65);
				ctmp = (char)itmp;
				return String.valueOf(ctmp);
			case 2:
				itmp = Math.round(Math.random() * 25 + 97);
				ctmp = (char)itmp;
				return String.valueOf(ctmp);
			default :
				itmp = Math.round(Math.random() * 9);
				return  itmp + "";
		}
	}

}
