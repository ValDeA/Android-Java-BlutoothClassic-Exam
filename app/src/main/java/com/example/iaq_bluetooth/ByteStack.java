package com.example.iaq_bluetooth;

import java.util.Arrays;

public class ByteStack {
  private int top;
  private int stackSize;
  private byte stackArr[];

  // 스택을 생성하는 생성자
  public ByteStack(int stackSize) {
    top = -1;    // 스택 포인터 초기화
    this.stackSize = stackSize;    // 스택 사이즈 설정
    stackArr = new byte[this.stackSize];    // 스택 배열 생성
  }

  // 스택이 비어있는 상태인지 확인
  public boolean isEmpty() {
    // 스택 포인터가 -1인 경우 데이터가 없는 상태이므로 true 아닌 경우 false를 return
    return (top == -1);
  }

  // 스택이 가득찬 상태인지 확인
  public boolean isFull() {
    // 스택 포인터가 스택의 마지막 인덱스와 동일한 경우 true 아닌 경우 false를 return
    return (top == this.stackSize-1);
  }

  // 스택에 데이터를 추가
  public void push(byte item) {
    if(isFull()) {
      System.out.println("Stack is full!");

      for(int i=0; i<stackArr.length-1; i++) {
        stackArr[i] = stackArr[i+1];
      }
      this.pop();
    }
    stackArr[++top] = item;    // 다음 스택 포인터가 가리키는 인덱스에 데이터 추가
    System.out.println("Inserted Item : " + item);
  }

  // 스택의 최상위(마지막) 데이터 추출 후 삭제
  public byte pop() {
    if(isEmpty()) {
      System.out.println("Deleting fail! Stack is empty!");
      return 0;
    } else {
      System.out.println("Deleted Item : " + stackArr[top]);
      return stackArr[top--];
    }
  }

  // 스택의 최상위(마지막) 데이터 추출
  public byte peek() {
    if(isEmpty()) {
      System.out.println("Peeking fail! Stack is empty!");
      return 0;
    } else {
      System.out.println("Peeked Item : " + stackArr[top]);
      return stackArr[top];
    }
  }

  public byte[] peekAllStack() {
    if(isEmpty()) {
      System.out.println("Peeking fail! Stack is empty!");
      return new byte[3];
    } else {
      System.out.println("Peeked All Item : " + Arrays.toString(stackArr));
      return stackArr;
    }
  }

  // 스택 초기화
  public void clear() {
    if(isEmpty()) {
      System.out.println("Stack is already empty!");
    } else {
      top = -1;    // 스택 포인터 초기화
      stackArr = new byte[this.stackSize];    // 새로운 스택 배열 생성
      System.out.println("Stack is clear!");
    }
  }

  // 스택에 저장된 모든 데이터를 출력
  public void printStack() {
    if(isEmpty()) {
      System.out.println("Stack is empty!");
    } else {
      System.out.print("Stack elements : ");
      for(int i=0; i<=top; i++) {
        System.out.print(stackArr[i] + " ");
      }
      System.out.println();
    }
  }
}

