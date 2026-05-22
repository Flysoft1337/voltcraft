#!/usr/bin/env python3
"""
生成水解槽 GUI 纹理占位符
"""

from PIL import Image, ImageDraw
import os

def create_gui_texture():
    # GUI 尺寸：176x166（标准 Minecraft GUI 尺寸）
    width, height = 176, 166

    # 创建图像
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 绘制背景（灰色）
    draw.rectangle([0, 0, width-1, height-1], fill=(198, 198, 198, 255))

    # 绘制边框
    draw.rectangle([0, 0, width-1, height-1], outline=(0, 0, 0, 255), width=2)

    # 绘制标题栏
    draw.rectangle([4, 4, width-5, 20], fill=(139, 139, 139, 255))

    # 绘制玩家背包区域
    draw.rectangle([7, 83, 169, 159], fill=(139, 139, 139, 255))

    # 绘制输入槽（上方）
    draw.rectangle([55, 16, 73, 34], fill=(139, 139, 139, 255), outline=(0, 0, 0, 255))

    # 绘制输出槽（右侧）
    draw.rectangle([115, 34, 133, 52], fill=(139, 139, 139, 255), outline=(0, 0, 0, 255))

    # 绘制能量条背景
    draw.rectangle([7, 15, 25, 63], fill=(0, 0, 0, 255))

    # 绘制进度箭头背景
    draw.rectangle([75, 34, 101, 52], fill=(139, 139, 139, 255))

    # 保存文件
    output_path = os.path.join(os.path.dirname(__file__),
                               '../src/main/resources/assets/voltcraft/textures/gui/electrolyzer.png')
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path)
    print(f"已生成 GUI 纹理：{output_path}")

if __name__ == "__main__":
    create_gui_texture()
