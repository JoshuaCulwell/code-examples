#include <msp430.h> 
#include <string.h>


/**
 * Joshua Culwell
 * EELE 371 - Final Project
 * STEPPER MOTOR:
 *      Full revolution : 516 steps
 *      Gear reduction = 1/16
 *      Output single revolution = 2064 "steps" for program
 *
 *      Source: https://www.adafruit.com/product/858#technical-details
 *
 *      5V
 * Timing:
 *      ((1000000/steps) * 25)/2 = how high to count for a 25 second revolution (open gate)
 *
 *      Max speed is just under 6 rpms -- 10 seconds for 1 revolution (close gate)
 *        - Source: https://www.adafruit.com/product/858#technical-details
 *
 *      ((1000000/steps) * 10)/2 = how high to count for a 10 second revolution (close gate)
 *
 *      (484.496124 * seconds)/2
 *      25 seconds ->   6056 -- ACTUAL TIME - ~23-24 seconds
 *      10 seconds ->   2422 -- ACTUAL TIME - ~9-10 seconds
 *
 *      each step cycle - OPEN = ~48 ms
 *      each step cycle - CLOSE = ~20 ms
 *
 *
 * ADC:
 *      1.5 mV/kg
 *      1300 kg car -> 1.95 V
 *      1600 kg car -> 2.4 V (max) --> won't fry the ADC if we hit this (can modify to be higher)
 *      172 kg person -> 0.25 V --> def shouldn't open
 *
 *      (1.95/3.3) * 2^8 = 151.27
 *
 *      Resolution error
 *      ((3.3 - 0) / 2^8) = 0.0129 V
 *
 *
 *
 * PIN DIAGRAM:
 *    OUTPUTS:
 *      PIN 6.0-6.3 Motor output
 *      PIN 1.3 SDA
 *      PIN 3.0 Lock Indicator
 *      PIN 4.6-4.7 Alert lights
 *    INPUTS:
 *      PIN 1.2 SCL
 *      PIN 5.3 "pressure plate" voltage input
 *
 * Extra Features
 *      The guardshack now can lock the gate after pressing "l" or "L" in the terminal.
 *          If the gate is actively opening it will close and lock. If it is already
 *          closed it will stay closed.
 *
 *      If there is any pressure the lights will stay on until there is no pressure
 *          Value used for the ADC is 45 because min power output from the AD2 is 0.5 V (so we'll use slightly over that).
 *          Idealy would use the ADC value of 15 --> 128 kg
 *
 */
void init()
{
	WDTCTL = WDTPW | WDTHOLD;	// stop watchdog timer
	//-------------------------------------------------
	//ADC config for "pressure plate"
	ADCCTL0 &= ~ADCSHT;         //Clear ADCSHT
    ADCCTL0 |= ADCSHT_2;        //Conversion Cycles = 16

    ADCCTL0 |= ADCON;           //Turn ACD on
    ADCCTL1 |= ADCSSEL_2;       //SMCLK
    ADCCTL1 |= ADCSHP;          //Sample signal source = sampling timer

    ADCCTL2 &= ~ADCRES;         //Clear ADCRES
    ADCCTL2 |= ADCRES_0;        //Resolution = 8 bit -- LOWEST

    ADCMCTL0 |= ADCINCH_11;     //ADC Input Channel = A11 (P5.3)

    ADCIE |= ADCIE0;            //Enable ADC Conv Complete IQR
    //----------------------------------------------------
    //Timer config for motor rotation speed
    TB0CTL |= TBCLR;            //Clear timer and dividers
    TB0CTL |= TBSSEL__SMCLK;    //Set as SMCLK (1MHz)
    TB0CTL |= MC__UP;           //Set as count up
    TB0CTL |= CNTL_0;           //Set as 2^16 (doesn't really matter though.)
    TB0EX0 &= ~TBCLR;           //Clear extra dividers
    TB0CTL |= ID__1;            //Set as divide by 8
    TB0EX0 |= 1;                //Set extra dividers as 1 (redundant)

    TB0CCTL0 &= ~CCIE;          //Disable Interrupt -- Enable later
    TB0CCTL0 &= ~CCIFG;         //Clear the flag
    //-------------------------------------------------------
    //I2C configuration
    UCB0CTLW0 |= UCSWRST;       //Put into reset mode

    UCB0CTLW0 |= UCSSEL_3;      //SMCLK 1 Mhz
    UCB0BRW = 10;               //Divide by 10 to get 100kHz

    UCB0CTLW0 |= UCMODE_3;      //Put into I^2C mode
    UCB0CTLW0 |= UCMST;         //Put into master mode
    UCB0CTLW0 |= UCTR;          //Put into Tx (send data) mode
    UCB0I2CSA = 0x0068;         //Slave address = 0x68

    UCB0CTLW1 |= UCASTP_2;      //Auto STOP when UCB0TBCNT reached
    UCB0TBCNT = 0x08;           //sizeof(packet); //# of bytes in packet

    //Configure ports
    P1SEL1 &= ~BIT3;            //P1.3 as SCL
    P1SEL0 |= BIT3;

    P1SEL1 &= ~BIT2;            //P1.2 as SDA
    P1SEL0 |= BIT2;

    UCB0CTLW0 &= ~UCSWRST;      //Take out of reset mode

    UCB0IE |= UCTXIE0;          //Enable I^2C Tx0 IRQ
    UCB0IE |= UCRXIE0;          //Enable I^2C Rx0 IRQ
    //-------------------------------------------------------------
    //UART configuration
    UCA1CTLW0 |= UCSWRST;       //Put eUSCI_A1 into software reset

    UCA1CTLW0 |= UCSSEL__SMCLK; //Set as small clock (1 MHz)
    UCA1BRW = 8;                //Output as byte
    UCA1MCTLW &= ~0xFFFF;       //Clear the baud rate
    UCA1MCTLW |= 0xD600;        //Baud Rate - 115200

    P4SEL1 &= ~BIT3;            //Makes output work
    P4SEL0 |= BIT3;             //^

    P4SEL1 &= ~BIT2;            //Makes input work -- "Guard Shack Override"
    P4SEL0 |= BIT2;             //^

    UCA1CTLW0 &= ~UCSWRST;      //Take eUSCI_A1 out of SW reset

    UCA1IE |= UCRXIE;           //Enable interrupt
    //--------------------------------------------------------------
    //Configure stepper motor output -- Pin 6.0-6.3
    P6DIR |= BIT0;
    P6OUT &= ~BIT0;
    P6DIR |= BIT1;
    P6OUT &= ~BIT1;
    P6DIR |= BIT2;
    P6OUT &= ~BIT2;
    P6DIR |= BIT3;
    P6OUT &= ~BIT3;
    //--------------------------------------------------------------
    //Guard Shack output
    P3DIR |= BIT0;
    P3OUT &= ~BIT0;
    //Alert light output
    P4DIR |= BIT6;
    P4DIR |= BIT7;
    P4OUT &= ~BIT6;
    P4OUT &= ~BIT7;


    PM5CTL0 &= ~UCSWRST;        //Disable high z
    __enable_interrupt();       //Enable interrupts

    //Set date
    UCB0CTLW0 |= UCTXSTT;
}
//Global variables/definitions----------------------------------------------------------
//Time
char time[] = {0x03, 0x30, 0x15, 0x01, 0x01, 0x5, 0x12, 0x22};//Holds current date/time Currently set as 12/5/22 @ 1:15:30
char received[] = {0, 0, 0, 0, 0, 0, 0};//Holds the values of the received date/time
unsigned int timeCnt = 0;                //Used to iterate through time array
int transmit = 1;               //Transmit is to transmit current date/time
//Motor/ADC
int moving;                     //Tracks if the gate is moving or not. Makes single closed/open outputs if pressure is close to threshold
int pressure;
int pressureThreshold = 151;    //ADC will read 151 when a 1300kg car drives on pressure plate
char gatePos = 'c';             //c for closed o for open.
char direction = 'o';           //o to open gate c to close gate
const int STEPS = 2064;         //Amount of steps for a full revolution (open <-> closed)
int stepCount = 0;              //Amount of steps that has been taken so far
unsigned int stepPos = 3;       //original step is 0011

const int timeCloseNum = 2422;  //10 seconds
const int timeOpenNum = 6056;  //25 seconds
//UART message
char *output;
char messageOpen[] = "\n\rGate opened at: ";
char messageClose[] = "\n\rGate closed at: ";
char messageLocked[] = "\n\rGate locked at: ";
char messageUnlocked[] = "\n\rGate unlocked at: ";
char messageTime[] = " on ";
unsigned int pos = 0;
unsigned int timePos = 0;
int n;
//Additional Features
//UART control
int lock = 0;
//--------------------------------------------------------------------------------------
void getTime(){
    UCB0CTLW0 |= UCTR;          //Send Address
    UCB0TBCNT = 0x01;           //Only 1
    UCB0CTLW0 |= UCTXSTT;       //Initiate

    while((UCB0IFG & UCSTPIFG) == 0);//While the flag is down wait
    UCB0IFG &= ~UCTXSTT;        //Clear the flag
    int i;                      //define i
    for(i=0;i<50;i++);          //Add delay

    UCB0CTLW0 &= ~ UCTR;        //Receive
    UCB0TBCNT = 0x07;           //7 -- the whole date/time
    UCB0CTLW0 |= UCTXSTT;       //Initiate

    while((UCB0IFG & UCSTPIFG) == 0);
    UCB0IFG &= ~UCSTPIFG;
}
void run(){
    ADCCTL0 |= ADCENC | ADCSC;
    //------------------------------------Pressure Lights - Additional Value--------------
    if(pressure > 45){
        P4OUT |= BIT6;
        P4OUT |= BIT7;
    }else{
        P4OUT &= ~BIT6;
        P4OUT &= ~BIT7;
    }

    //------------------------------------Car is present----------------------------------
    if(pressure >= pressureThreshold && moving == 0 && lock == 0){
        if(gatePos == 'c'){
            direction = 'o';
            TB0CCR0 = timeOpenNum;//Speed to open
            TB0CCTL0 |= CCIE;//Enable timer to open gate
            //Generate message--------------
            getTime();
            output = messageOpen;
            n = sizeof(messageOpen) - 1;
            UCA1IE |= UCTXCPTIE;                    //Enable UART interrupt
            UCA1IFG &= ~UCTXCPTIFG;                 //Clear the flag ^
            UCA1TXBUF = output[0];
            //------------------------------
            while(pressure >= (pressureThreshold - 20) && lock == 0) ADCCTL0 |= ADCENC | ADCSC;//Hold it open if car is still present
        }
    //----------------------------------Car is not present--------------------------------
    }else if(moving == 0){
        if(gatePos == 'o'){
            direction = 'c';
            TB0CCR0 = timeCloseNum;                 //Speed to close
            TB0CCTL0 |= CCIE;                       //Enable timer to close gate
            //generate message-------------
            getTime();
            output = messageClose;
            n = sizeof(messageClose) - 1;
            UCA1IE |= UCTXCPTIE;                    //Enable UART interrupt
            UCA1IFG &= ~UCTXCPTIFG;                 //Clear the flag ^
            UCA1TXBUF = output[0];
            while(gatePos == 'o'){}
            //------------------------------
        }
    }
}

int main(void){
    init();
    while(1){
        run();
    }
	return 0;
}

//Interrupt Service Routines ------------------------------------------------

//ADC "pressure plate" update value
#pragma vector = ADC_VECTOR
__interrupt void ADC_ISR(void){
    pressure = ADCMEM0;        //Update ADC_Value integer
//------------END ISR----------------------------------------
}

//Timer to open/close gate
#pragma vector = TIMER0_B0_VECTOR                   //Interrupt vector for the first main timer
__interrupt void ISR_Timer_1(void){                 //Function ISR

    moving = 1;                 //Indicate that the gate is actively moving

    if(lock == 1 && direction == 'o'){
        direction = 'c';
        stepCount = STEPS - stepCount;

        output = messageClose;
        n = sizeof(messageClose) - 1;
        UCA1TXBUF = output[0];
    }
    stepCount ++;               //Increase step count
    if(direction == 'o'){       //if it needs to open
        stepPos <<= 1;          //Left shift bits
        if(stepPos & 16){       //if the bit in 16 is active
            stepPos |= 1;       //Set bit in 1
            stepPos &= ~16;     //clear bit in 16
        }
    }
    if(direction == 'c'){       //If it needs to close
        if(stepPos & 1) stepPos |= 16;//If we are at bit 1 set bit 16
        stepPos >>= 1;          //right shift the bits
    }
    P6OUT &= stepPos;           //clear the bits we don't want
    P6OUT |= stepPos;           //set the bits we do want
    if(stepCount >= STEPS){     //If we have reached a full revolution
        TB0CCTL0 &= ~CCIE;      //Disable interrupt
        stepCount = 0;          //Set step count to 0
        gatePos = (gatePos == 'o' || lock == 1) ? 'c' : 'o';
        moving = 0;             //Indicate that the gate is not moving
    }

//-------------------------------------------------END ISR_Timer_1
    TB0CCTL0 &= ~CCIFG;                             //Clear the flag
}

//I2C protocol -- send once then only receive.
#pragma vector = EUSCI_B0_VECTOR
__interrupt void EUSCI_B0_I2C_ISR(void){
    if(UCB0CTLW0 & UCTR && transmit == 1){              //If it is in transmit mode
        UCA0IFG &= ~UCRXIFG;                            //Clear flag
        if(timeCnt == (sizeof(time) - 1)){              //If we are at the end of the packet to transmit
            UCB0TXBUF = time[timeCnt];                  //Transmit final one
            timeCnt = 0;                                //Reset dataCnt
            UCB0CTLW0 &= ~UCTR;                         //set to receive mode
            transmit = 0;                               //Set transmit to 0 so we no longer transmit
        }else{
            UCB0TXBUF = time[timeCnt];                  //if we haven't transmitted everything transmit portion of packet
            timeCnt ++;                                 //Increase what to transmit
        }
    }else{//If we aren't in transmit mode
        switch(UCB0IV){
        case 0x16:              //If the bits are set to read
            received[timeCnt] = UCB0RXBUF; //Read it (Which clears the flag)
            timeCnt = (timeCnt == (sizeof(received)-1)) ? 0 : timeCnt + 1;
            break;              //break
        case 0x18:              //If the bits are set to write
            UCB0TXBUF = 0x03;   //Write the address to then receive (which also clears the flag)
            break;              //break
        default: break;         //If for some reason it's something else then do nothing.
        }
    }
    //----------------------------------------------END EUSCI_B0_I2C_ISR
}

//UART interrupt
#pragma vector = EUSCI_A1_VECTOR
__interrupt void ISR_EUSCI_A1(void){        //UART interrupt
    if(UCA1IFG & UCTXCPTIFG){
        if(pos == n){
            if(timePos < 2){
                int i;
                if(timePos == 0){
                    //HOURS
                    UCA1TXBUF = ((received[2] & 0xF0)>>4) + '0'; // Prints the 10s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = (received[2] & 0x0F) + '0'; // Prints the 1s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = ':'; // Newline character
                    //MINUTES
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = ((received[1] & 0xF0)>>4) + '0'; // Prints the 10s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = (received[1] & 0x0F) + '0'; // Prints the 1s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = ':'; // Newline character
                    //SECONDS
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = ((received[0] & 0xF0)>>4) + '0'; // Prints the 10s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = (received[0] & 0x0F) + '0'; // Prints the 1s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = ' '; // Newline character
                    timePos = 1;

                    output = messageTime;
                    n = sizeof(messageTime) - 1;
                    pos = 0;
                }else{
                    //MONTH
                    UCA1TXBUF = ((received[5] & 0xF0)>>4) + '0'; // Prints the 10s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = (received[5] & 0x0F) + '0'; // Prints the 1s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = '/'; // Newline character
                    //DAY
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = ((received[3] & 0xF0)>>4) + '0'; // Prints the 10s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = (received[3] & 0x0F) + '0'; // Prints the 1s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = '/'; // Newline character
                    //YEAR
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = ((received[6] & 0xF0)>>4) + '0'; // Prints the 10s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = (received[6] & 0x0F) + '0'; // Prints the 1s digit
                    for (i = 0; i < 500; i++){}
                    UCA1TXBUF = ' '; // Newline character
                    timePos = 2;
                }
            }else{
                pos = 0;
                timePos = 0;
                UCA1IE &= ~UCTXCPTIE;      //If position is at the end disable interrupt
            }
        }else{
            UCA1TXBUF = output[++pos];            //Update output to the position
        }
    }
    //Guard shack controls
    if(UCA1IFG & UCRXIFG){
        if(UCA1RXBUF == 'l' || UCA1RXBUF == 'L'){
            lock = lock == 1 ? 0 : 1; //Toggle lock if l or L is pressed
            P3OUT ^= BIT0;
            if(lock == 1){
                output = messageLocked;
                n = sizeof(messageLocked) - 1;
            }else{
                output = messageUnlocked;
                n = sizeof(messageUnlocked) - 1;
            }
            UCA1IE |= UCTXCPTIE;                    //Enable UART interrupt
            UCA1TXBUF = output[0];
        }
    }
//------------------------------------------END ISR_EUSCI_A1----
    UCA1IFG &= ~UCTXCPTIFG;                 //Clear flag
}


